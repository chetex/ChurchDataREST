package com.chetex.church.rest.service;

// Entity + repository used to persist the latest fingerprint observed on the source website.
import com.chetex.church.rest.entity.ContentFingerprint;
import com.chetex.church.rest.repository.ContentFingerprintRepository;

// Jsoup types used to load the home page and navigate the parsed DOM.
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Logging facade (SLF4J) for operational visibility without binding to a specific implementation.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring injection of configuration properties + service stereotype annotation.
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
// Transactional boundary so fingerprint reads/writes run within a single DB transaction.
import org.springframework.transaction.annotation.Transactional;

// IO + crypto + time types needed to fetch the HTML and compute the SHA-256 fingerprint.
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for answering the question "are there new elements on
 * the source website since the last time we checked?".
 *
 * <p>The strategy is intentionally <b>lightweight</b>: the home page HTML is
 * downloaded once, a stable list of post identifiers (link + title) is
 * collected, sorted and hashed with SHA-256. The resulting hash is compared
 * to the one stored in the database. A mismatch means new, updated or
 * removed posts, and the stored fingerprint is refreshed so the next check
 * starts from the latest known state.</p>
 */
@Service // Marks the class as a Spring-managed service bean.
public class StatusService {

    // Logger used to track whether the home page changed and why.
    private static final Logger log = LoggerFactory.getLogger(StatusService.class);

    // Desktop User-Agent to avoid being rejected by anti-bot filters. Mirrors WebScrapingService.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Fixed timeout to keep the endpoint fast and predictable even if the site is slow.
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    // Logical scope stored in the "content_fingerprint" table for the home page check.
    private static final String HOME_SCOPE = "home";

    // URL of the Catholic parish website injected from application.properties (church.url).
    private final String churchUrl;

    // Spring Data repository that persists the latest observed fingerprint.
    private final ContentFingerprintRepository fingerprintRepository;

    // Generic JSON cache facade; wiped whenever a new fingerprint is detected.
    private final CacheService cacheService;

    /**
     * Constructor injection: Spring wires the repositories, the cache facade
     * and the configured URL, keeping the class easy to unit-test.
     */
    public StatusService(@Value("${church.url}") String churchUrl,
                         ContentFingerprintRepository fingerprintRepository,
                         CacheService cacheService) {
        this.churchUrl = churchUrl; // Store the source URL for later scraping.
        this.fingerprintRepository = fingerprintRepository; // Store the repository used for persistence.
        this.cacheService = cacheService; // Store the cache facade so we can invalidate it on change.
    }

    /**
     * Main entry point used by the controller.
     *
     * @return {@code true} when the home page fingerprint differs from the
     *         previously stored one (i.e. fresh scraping is needed) and
     *         {@code false} when nothing changed (i.e. cached JSON can be
     *         served directly).
     * @throws IOException if the home page cannot be fetched.
     */
    @Transactional // Wraps find + save in a single transaction for consistency.
    public boolean areNewElements() throws IOException {
        String freshHash = computeHomeFingerprint(); // Fetch + hash the current home page state.
        Optional<ContentFingerprint> stored = fingerprintRepository.findById(HOME_SCOPE); // Look up last known hash.

        // First run ever: persist the fingerprint, wipe any stale cache and answer "true" so clients force an initial scrape.
        if (stored.isEmpty()) {
            fingerprintRepository.save(new ContentFingerprint(HOME_SCOPE, freshHash, Instant.now()));
            cacheService.invalidateAll(); // Fresh boot: discard any leftover rows from a previous schema.
            log.info("No previous fingerprint stored; treating site as containing new elements.");
            return true;
        }

        ContentFingerprint existing = stored.get(); // Unwrap the Optional since we know it is present.

        // When the hashes match, the site is unchanged and no further scraping is required.
        if (existing.getHashValue().equals(freshHash)) {
            log.info("Fingerprint unchanged for scope '{}': no new elements.", HOME_SCOPE);
            return false;
        }

        // Hash differs: record the new one, invalidate the cache and return true so endpoints re-scrape on next call.
        existing.update(freshHash, Instant.now()); // Mutate the managed entity; Hibernate will flush it.
        cacheService.invalidateAll(); // Drop every cached JSON row so follow-up endpoints rebuild them from scratch.
        log.info("Fingerprint changed for scope '{}': new elements detected and cache invalidated.", HOME_SCOPE);
        return true;
    }

    /**
     * Downloads the home page and builds a deterministic SHA-256 fingerprint
     * based on the post titles and absolute links visible on it.
     */
    private String computeHomeFingerprint() throws IOException {
        log.debug("Fetching home page for fingerprint: {}", churchUrl); // Operational breadcrumb.
        Document document = Jsoup.connect(churchUrl)                    // Open a Jsoup connection to the home URL.
                .userAgent(USER_AGENT)                                  // Announce a desktop browser identity.
                .timeout(CONNECT_TIMEOUT_MS)                            // Fail fast if the server is unresponsive.
                .get();                                                 // Execute the GET request and parse HTML.

        List<String> signatureTokens = collectPostSignatures(document); // Extract a stable post signature list.

        // If no posts were found, fall back to hashing the whole HTML body: still deterministic.
        if (signatureTokens.isEmpty()) {
            Element body = document.body();                             // The <body> element (may be null on empty docs).
            String fallback = body != null ? body.html() : "";          // Convert the body to a string for hashing.
            log.warn("No post signatures detected on home page; hashing raw body as fallback.");
            return sha256Hex(fallback);                                 // Fallback fingerprint keeps the endpoint usable.
        }

        Collections.sort(signatureTokens);                              // Sort tokens to make order independent of DOM layout.
        String joined = String.join("|", signatureTokens);              // Join with a separator unlikely to appear in URLs.
        return sha256Hex(joined);                                       // Final fingerprint of the home page state.
    }

    /**
     * Extracts stable per-post signatures from the home page.
     *
     * <p>Each token is composed of the absolute post URL (from the title
     * anchor) plus the post title. URL + title together are resilient to
     * minor DOM reorderings and detect both new posts and title edits.</p>
     */
    private List<String> collectPostSignatures(Document document) {
        List<String> tokens = new ArrayList<>(); // Container for the generated signature strings.

        // Colibri WP (the site builder used by the parish) wraps every post with ".hentry".
        Elements posts = document.select(".hentry"); // Same selector used by HomeContentScrapingStrategy.

        for (Element post : posts) {                                    // Iterate over each detected post element.
            Element titleEl = post.selectFirst(".h-blog-title");        // The Colibri-specific post title container.
            if (titleEl == null) continue;                              // Defensive: skip malformed posts without a title.

            Element anchor = titleEl.selectFirst("a");                  // Anchor element holding the canonical post URL.
            String title = titleEl.text();                              // Visible title text, trimmed by Jsoup already.
            String url = anchor != null ? anchor.attr("abs:href") : ""; // Absolute URL; empty string if the anchor is missing.

            // Only keep tokens with at least one meaningful field to avoid polluting the hash with blanks.
            if ((title == null || title.isBlank()) && (url == null || url.isBlank())) continue;

            tokens.add((url == null ? "" : url) + "::" + (title == null ? "" : title.trim()));
        }

        return tokens; // Caller will sort and hash these tokens.
    }

    /**
     * Computes the SHA-256 hash of the provided text and returns it as a
     * lowercase hexadecimal string, suitable for storage and comparison.
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");     // SHA-256 is widely available in the JDK.
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8)); // Hash the UTF-8 bytes of the input.
            StringBuilder hex = new StringBuilder(bytes.length * 2);         // Pre-size builder for efficiency.
            for (byte b : bytes) {                                           // Walk every byte of the digest output.
                hex.append(String.format("%02x", b));                        // Append the two-character hex representation.
            }
            return hex.toString();                                           // Final lowercase hex digest.
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is part of the JDK so this branch should never fire; fail loudly if it does.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
