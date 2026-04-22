package com.chetex.church.rest.dto;

/**
 * Respuesta del endpoint de login. La app móvil espera un campo
 * {@code status} con valor "OK" o "FAIL".
 *
 * @param status "OK" si las credenciales son correctas; "FAIL" si no.
 * @param role   Rol del usuario cuando {@code status = OK} (null si FAIL).
 */
public record LoginResponseDTO(String status, String role) {
}
