package com.chetex.church.rest.dto;

import java.util.List;

/**
 * Respuesta compuesta del endpoint {@code /api/horarios}.
 *
 * <p>Reúne:
 * <ul>
 *   <li>Título principal de la página ({@link #title}).</li>
 *   <li>Tres enlaces destacados (si existen): horario de verano, invierno y
 *       horario conjunto de todas las parroquias de Tres Cantos.</li>
 *   <li>Lista de secciones con título, subtítulo, texto e imagen
 *       (horarios por servicio: Despacho, Eucaristías, Confesiones…).</li>
 * </ul></p>
 *
 * @param title               Título principal de la página (h1).
 * @param linkHorarioVerano   URL del horario de verano (puede ser null).
 * @param linkHorarioInvierno URL del horario de invierno (puede ser null).
 * @param linkTresCantos      URL del horario conjunto de Tres Cantos (puede ser null).
 * @param sections            Bloques de contenido de la página.
 */
public record HorarioPageDTO(
        String title,
        String linkHorarioVerano,
        String linkHorarioInvierno,
        String linkTresCantos,
        List<PageSectionDTO> sections
) {
}
