package com.chetex.church.rest.repository;

import com.chetex.church.rest.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio JPA para {@link AppUser}. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
