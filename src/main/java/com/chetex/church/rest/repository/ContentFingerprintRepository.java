package com.chetex.church.rest.repository;

// Entity managed by this repository.
import com.chetex.church.rest.entity.ContentFingerprint;
// Base Spring Data interface offering CRUD + paging operations.
import org.springframework.data.jpa.repository.JpaRepository;
// Marks the interface as a Spring-managed repository bean.
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link ContentFingerprint} rows.
 *
 * <p>The primary-key type is {@link String} because fingerprints are keyed
 * by a logical scope such as {@code "home"}.</p>
 */
@Repository // Registers the interface as a repository bean.
public interface ContentFingerprintRepository extends JpaRepository<ContentFingerprint, String> {
    // Default CRUD operations from JpaRepository are enough for now.
}
