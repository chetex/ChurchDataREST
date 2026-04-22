package com.chetex.church.rest.entity;

// JPA annotations for entity mapping.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Timestamp type for the "last checked" column.
import java.time.Instant;

/**
 * JPA entity that stores a lightweight fingerprint (hash) of the source
 * website content, used to decide whether new elements appeared since the
 * last visit. One row per scope (for example {@code "home"} or
 * {@code "page:<url>"}): comparing the stored hash against a freshly
 * computed one tells us whether re-scraping is required.
 */
@Entity // Registers the class as a JPA-managed entity.
@Table(name = "content_fingerprint") // Maps the entity to the "content_fingerprint" SQL table.
public class ContentFingerprint {

    /** Scope identifier that groups fingerprints (e.g. "home"). Used as primary key. */
    @Id // Primary-key annotation.
    @Column(name = "scope", nullable = false, length = 128) // Short descriptive key; 128 chars is plenty.
    private String scope;

    /** Hex-encoded hash (SHA-256) summarising the observed content. */
    @Column(name = "hash_value", nullable = false, length = 128) // SHA-256 hex is 64 chars; 128 leaves headroom.
    private String hashValue;

    /** Last moment the fingerprint was computed/updated. */
    @Column(name = "updated_at", nullable = false) // Timestamp of the latest freshness check.
    private Instant updatedAt;

    /** No-args constructor required by JPA. */
    protected ContentFingerprint() {
        // Intentionally empty: JPA only.
    }

    /** Convenience constructor used when inserting a brand-new fingerprint row. */
    public ContentFingerprint(String scope, String hashValue, Instant updatedAt) {
        this.scope = scope; // Store the logical scope (e.g. "home").
        this.hashValue = hashValue; // Store the computed content hash.
        this.updatedAt = updatedAt; // Record when the hash was computed.
    }

    public String getScope() { return scope; } // Getter for the scope primary key.

    public String getHashValue() { return hashValue; } // Getter for the stored hash.

    public Instant getUpdatedAt() { return updatedAt; } // Getter for the freshness timestamp.

    /** Updates the hash value and the timestamp in a single call. */
    public void update(String newHash, Instant when) {
        this.hashValue = newHash; // Replace the stored hash with the freshly computed one.
        this.updatedAt = when; // Bump the timestamp to the current check moment.
    }
}
