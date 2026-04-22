package com.chetex.church.rest.dto;

/**
 * Body del endpoint {@code POST /api/login/login-user}.
 *
 * @param user     Nombre de usuario.
 * @param password Contraseña en texto plano (nunca se persiste).
 */
public record LoginRequestDTO(String user, String password) {
}
