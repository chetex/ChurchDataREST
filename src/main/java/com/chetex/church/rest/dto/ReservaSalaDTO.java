package com.chetex.church.rest.dto;

import java.time.LocalDate;

/**
 * DTO usado tanto para exponer reservas existentes como para recibir una
 * nueva solicitud de reserva desde el cliente móvil.
 *
 * @param id     Identificador de la reserva (null en solicitudes entrantes).
 * @param sala   Nombre de la sala.
 * @param fecha  Fecha en formato ISO-8601 (YYYY-MM-DD).
 * @param hora   Franja horaria libre, p. ej. "19:00-20:30".
 * @param grupo  Nombre del grupo solicitante.
 * @param estado Estado actual: PENDING, OK o DENY (null en solicitudes entrantes).
 */
public record ReservaSalaDTO(
        Long id,
        String sala,
        LocalDate fecha,
        String hora,
        String grupo,
        String estado
) {
}
