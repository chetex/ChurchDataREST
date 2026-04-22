package com.chetex.church.rest.controller;

// DTO used as the HTTP response payload for the status endpoint.
import com.chetex.church.rest.dto.NewElementsStatusDTO;
// Services: fingerprint decision + cache facade (for manual invalidation).
import com.chetex.church.rest.service.CacheService;
import com.chetex.church.rest.service.StatusService;

// Spring Web annotations used to declare the REST endpoint.
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// IOException is propagated if the underlying scrape fails.
import java.io.IOException;
// Timestamp emitted in the JSON response for client-side diagnostics.
import java.time.Instant;

/**
 * REST controller exposing cache-control endpoints under {@code /api/status}.
 *
 * <p>The mobile application must call {@code GET /api/status/new-elements}
 * before any other request: its boolean response drives whether the client
 * (and the server) should rely on cached JSON payloads or trigger a fresh
 * scrape.</p>
 */
@RestController // Declares the class as a JSON-producing Spring controller.
@RequestMapping("/api/status") // Namespaces all endpoints in this controller under /api/status.
public class StatusController {

    // Service bean responsible for fetching + hashing the home page and comparing with DB state.
    private final StatusService statusService;

    // Cache facade exposed to allow manual invalidation via HTTP (admin/debug endpoint).
    private final CacheService cacheService;

    /**
     * Constructor injection keeps the controller stateless and testable.
     */
    public StatusController(StatusService statusService, CacheService cacheService) {
        this.statusService = statusService; // Store the injected status service.
        this.cacheService = cacheService;   // Store the cache facade for the invalidate endpoint.
    }

    /**
     * Critical lightweight endpoint queried by the mobile app to decide
     * whether cached JSON responses can be reused.
     *
     * @return a {@link NewElementsStatusDTO} with a boolean flag and the
     *         timestamp of the check.
     * @throws IOException if the source website cannot be reached.
     */
    @GetMapping("/new-elements") // Maps HTTP GET /api/status/new-elements to this handler.
    public NewElementsStatusDTO areNewElements() throws IOException {
        boolean hasNew = statusService.areNewElements();         // Delegate the decision to the service layer.
        return new NewElementsStatusDTO(hasNew, Instant.now().toString()); // Wrap the answer with a timestamp.
    }

    /**
     * Admin/debug endpoint that wipes every cached JSON row so the next
     * call to any endpoint forces a fresh scrape. Useful after changing a
     * scraping strategy: the fingerprint hash on the source site may be
     * unchanged while the persisted payload needs to be regenerated.
     */
    @DeleteMapping("/cache") // Maps HTTP DELETE /api/status/cache to this handler.
    public NewElementsStatusDTO invalidateCache() {
        cacheService.invalidateAll();                                 // Remove every row from cached_response.
        return new NewElementsStatusDTO(true, Instant.now().toString()); // Report success with a timestamp.
    }
}
