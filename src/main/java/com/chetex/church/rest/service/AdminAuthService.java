package com.chetex.church.rest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guardia de autorización mínima para operaciones administrativas:
 * decisiones de reserva y push notifications.
 *
 * <p>Espera una cabecera {@code X-Admin-Secret} que coincida con la
 * propiedad {@code admin.secret}. Si falta o no cuadra, devuelve 401.
 * Es intencionadamente simple; se migrará a Spring Security + JWT en
 * siguiente iteración.</p>
 */
@Service
public class AdminAuthService {

    private final String configuredSecret;

    public AdminAuthService(@Value("${admin.secret}") String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    /**
     * Verifica la cabecera admin. Lanza 401 si falta o no coincide.
     */
    public void requireAdmin(String headerSecret) {
        if (headerSecret == null || headerSecret.isBlank() || !headerSecret.equals(configuredSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial admin inválida");
        }
    }
}
