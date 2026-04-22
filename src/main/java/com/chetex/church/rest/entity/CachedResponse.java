package com.chetex.church.rest.entity;

// JPA annotations to declare the entity mapping against a PostgreSQL table.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Standard Java time type used for the "updated at" timestamp column.
import java.time.Instant;

/**
 * JPA entity that stores the JSON payload returned by each endpoint.
 *
 * <p>The primary key is a logical cache key (for example {@code "home.menu"},
 * {@code "home.elements"}, {@code "socials"} or {@code "page.content:<url>"}).
 * The full JSON response is persisted as text so that, when no new elements
 * are detected on the source website, the API can serve the cached JSON
 * directly without re-scraping.</p>
 */
@Entity // Marks this class as a JPA entity managed by the persistence context.
@Table(name = "cached_response") // Maps the entity to the "cached_response" SQL table.
public class CachedResponse {

    /** Logical cache key used as the primary key (e.g. "home.menu"). */
    @Id // Declares this field as the primary key of the table.
    @Column(name = "cache_key", nullable = false, length = 512) // PK column; 512 chars fit URL-based keys.
    private String cacheKey;

    /** Full serialised JSON response stored verbatim to be served as-is on cache hits. */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT") // TEXT handles arbitrarily large JSON.
    private String payload;

    /** Last time this cache entry was refreshed from the source website. */
    @Column(name = "updated_at", nullable = false) // Timestamp column; never null so cache age is always known.
    private Instant updatedAt;

    /** No-args constructor required by JPA to hydrate entities via reflection. */
    protected CachedResponse() {
        // Intentionally empty: JPA only.
    }

    /** Convenience constructor used by the service layer when inserting/updating rows. */
    public CachedResponse(String cacheKey, String payload, Instant updatedAt) {
        this.cacheKey = cacheKey; // Assign the logical key that identifies the cached endpoint.
        this.payload = payload; // Assign the JSON body to persist.
        this.updatedAt = updatedAt; // Record when the payload was captured.
    }

    public String getCacheKey() { return cacheKey; } // Simple getter exposing the primary key.

    public String getPayload() { return payload; } // Simple getter exposing the stored JSON.

    public Instant getUpdatedAt() { return updatedAt; } // Simple getter exposing the freshness timestamp.

    /** Replaces the stored JSON and bumps the freshness timestamp atomically. */
    public void refresh(String newPayload, Instant when) {
        this.payload = newPayload; // Overwrite the cached JSON with the freshly scraped payload.
        this.updatedAt = when; // Bump the timestamp so downstream consumers know the cache is fresh.
    }
}
