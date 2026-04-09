package com.chetex.church.rest.dto;

/**
 * Elemento de contenido extraído de una página (home o subpágina del menú).
 *
 * Es un {@code record} para garantizar inmutabilidad por defecto (regla 8 del CONTEXT.md).
 *
 * @param title     Título principal del bloque.
 * @param text      Texto/extracto asociado al bloque.
 * @param image     URL absoluta de la imagen representativa (puede ser null).
 * @param linkUrl   URL absoluta del enlace asociado al bloque (puede ser null).
 * @param date      Fecha de publicación tal y como aparece en la web (puede ser null).
 * @param sourceUrl URL de la página de la que se extrajo el elemento (para trazabilidad).
 */
public record HomeContentItemDTO(
        String title,
        String text,
        String image,
        String linkUrl,
        String date,
        String sourceUrl
) {
}
