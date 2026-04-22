package com.chetex.church.rest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario de la app móvil. Se persiste en la tabla {@code app_user}.
 *
 * <p>El nombre {@code AppUser} evita colisión con la palabra reservada
 * {@code user} de PostgreSQL. La contraseña se almacena hasheada con BCrypt
 * en {@code password_hash}; nunca se persiste ni se retorna en texto plano.</p>
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario único (login). */
    @Column(nullable = false, length = 64, unique = true)
    private String username;

    /** Hash BCrypt de la contraseña. */
    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    /** Rol de la cuenta (USER / ADMIN). */
    @Column(nullable = false, length = 16)
    private String role = "USER";

    public AppUser() {}

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
}
