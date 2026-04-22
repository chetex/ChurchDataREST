package com.chetex.church.rest.repository;

// Parent entity managed by this repository.
import com.chetex.church.rest.entity.CachedResponse;
// Spring Data base interface that provides CRUD and paging operations out of the box.
import org.springframework.data.jpa.repository.JpaRepository;
// Marks the interface as a Spring-managed repository bean (optional with Spring Data but explicit here).
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link CachedResponse} rows.
 *
 * <p>The primary key type is {@link String} because each cached entry is
 * keyed by a logical endpoint identifier such as {@code "home.menu"}.</p>
 */
@Repository // Registers the interface as a repository bean discoverable by component scanning.
public interface CachedResponseRepository extends JpaRepository<CachedResponse, String> {
    // No custom queries needed yet: JpaRepository already exposes findById / save / deleteById.
}
