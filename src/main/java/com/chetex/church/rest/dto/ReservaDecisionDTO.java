package com.chetex.church.rest.dto;

/**
 * Cuerpo del endpoint admin {@code POST /api/groups/book/decision}. Decide
 * si una reserva se aprueba (OK) o se deniega (DENY).
 *
 * @param id       ID de la reserva afectada.
 * @param decision "OK" o "DENY" (case-insensitive).
 */
public record ReservaDecisionDTO(
        Long id,
        String decision
) {
}
