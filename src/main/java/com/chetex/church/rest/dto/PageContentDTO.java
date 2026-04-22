package com.chetex.church.rest.dto;

// Lista de bloques de contenido devueltos por el endpoint /api/page/content.
import java.util.List;

/**
 * Respuesta agregada del endpoint {@code GET /api/page/content?url=...}.
 *
 * @param url    URL absoluta de la página scrapeada (echo del parámetro de entrada).
 * @param title  Título de la página (contenido del elemento {@code <title>} o del primer h1).
 * @param blocks Lista ordenada de {@link ContentBlockDTO} con el contenido estructurado.
 */
public record PageContentDTO(
        String url,
        String title,
        List<ContentBlockDTO> blocks
) {
}
