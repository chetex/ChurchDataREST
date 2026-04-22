package com.chetex.church.rest.service;

// Jackson core types used to convert DTOs to/from JSON strings stored in PostgreSQL.
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// Persistence entity + repository backing the cache rows.
import com.chetex.church.rest.entity.CachedResponse;
import com.chetex.church.rest.repository.CachedResponseRepository;

// SLF4J logger for operational visibility.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring service stereotype + transactional management of read/write paths.
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Standard Java types used for timestamps and optional lookups.
import java.time.Instant;
import java.util.Optional;

/**
 * Generic JSON cache facade on top of {@link CachedResponseRepository}.
 *
 * <p>Endpoints delegate their "fetch or scrape" logic to this service: given
 * a cache key and a supplier that knows how to rebuild the payload from the
 * source website, {@link #getOrRefresh} returns the cached DTO when present
 * and otherwise scrapes, serialises the fresh payload into the database and
 * returns it.</p>
 */
@Service // Marks the class as a Spring-managed service bean.
public class CacheService {

    // Logger used to report cache hits, misses and deserialisation errors.
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    // Repository giving CRUD access to the cached_response table.
    private final CachedResponseRepository cacheRepository;

    // Jackson ObjectMapper (autoconfigured by Spring Boot) reused for every (de)serialisation.
    private final ObjectMapper objectMapper;

    /**
     * Functional interface that mirrors {@link java.util.function.Supplier}
     * but also allows checked {@link java.io.IOException}s thrown by Jsoup.
     */
    @FunctionalInterface // Enables lambda usage despite the checked exception signature.
    public interface ScraperSupplier<T> {
        /** Produces a fresh DTO by scraping the source website. */
        T get() throws java.io.IOException; // Method signature that callers must implement or lambda-ify.
    }

    /**
     * Constructor injection keeps the service testable and its dependencies explicit.
     */
    public CacheService(CachedResponseRepository cacheRepository, ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository; // Store the repository used for lookups + writes.
        this.objectMapper = objectMapper; // Store the Jackson mapper shared across calls.
    }

    /**
     * Returns the DTO stored under {@code cacheKey}, or rebuilds it via the
     * provided {@code scraper} and persists the result before returning it.
     *
     * @param cacheKey stable identifier of the cached endpoint (e.g. "home.menu").
     * @param typeRef  Jackson type reference describing the DTO shape (supports generics).
     * @param scraper  function called only on cache miss to produce a fresh DTO.
     * @param <T>      payload type returned to the caller.
     * @return the cached (or freshly scraped) DTO instance.
     * @throws java.io.IOException if the scraper itself fails.
     */
    @Transactional // Wraps lookup + optional save in a single DB transaction.
    public <T> T getOrRefresh(String cacheKey,
                              TypeReference<T> typeRef,
                              ScraperSupplier<T> scraper) throws java.io.IOException {
        Optional<CachedResponse> cached = cacheRepository.findById(cacheKey); // Look up existing cache row by key.

        if (cached.isPresent()) {                                              // Branch taken when the cache is warm.
            try {
                T payload = objectMapper.readValue(cached.get().getPayload(), typeRef); // Deserialise stored JSON.
                log.info("Cache HIT for key '{}' (updated at {})",                     // Operational breadcrumb.
                        cacheKey, cached.get().getUpdatedAt());
                return payload;                                                 // Return the cached DTO without scraping.
            } catch (JsonProcessingException e) {
                // Corrupted or schema-incompatible JSON: log, discard and fall through to a re-scrape.
                log.warn("Failed to deserialise cached payload for key '{}': {}. Falling back to scrape.",
                        cacheKey, e.getMessage());
            }
        }

        log.info("Cache MISS for key '{}'. Scraping fresh data…", cacheKey);   // Indicate that scraping will happen now.
        T freshPayload = scraper.get();                                        // Invoke the scraper to build a fresh DTO.
        storePayload(cacheKey, freshPayload);                                  // Persist the serialised DTO for future calls.
        return freshPayload;                                                   // Return the freshly scraped DTO to the caller.
    }

    /**
     * Serialises the given DTO to JSON and inserts or updates the cache row.
     */
    private <T> void storePayload(String cacheKey, T payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);             // Convert the DTO tree into a JSON string.

            // Re-use an existing row when possible to preserve the primary key without a delete+insert cycle.
            Optional<CachedResponse> existing = cacheRepository.findById(cacheKey);
            if (existing.isPresent()) {
                existing.get().refresh(json, Instant.now());                    // Managed entity: update via mutator.
            } else {
                cacheRepository.save(new CachedResponse(cacheKey, json, Instant.now())); // Fresh insert when absent.
            }
        } catch (JsonProcessingException e) {
            // Serialisation should not fail for DTOs, but log loudly instead of breaking the response.
            log.error("Failed to serialise payload for cache key '{}': {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Removes every cached row. Called by {@link StatusService} when a new
     * fingerprint is observed and all stored JSON payloads must be rebuilt.
     */
    @Transactional // Ensures the truncation happens atomically.
    public void invalidateAll() {
        long removed = cacheRepository.count();            // Count rows for the log line before deletion.
        cacheRepository.deleteAllInBatch();                 // Single SQL statement: fastest way to wipe the table.
        log.info("Cache invalidated: {} row(s) removed.", removed);
    }
}
