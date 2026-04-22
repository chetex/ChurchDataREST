package com.chetex.church.rest.dto;

/**
 * Sección genérica de una página estática (Colibri WP): un {@code h-heading}
 * con su texto asociado y (opcionalmente) una imagen y subtítulo.
 *
 * <p>Se usa como unidad de respuesta para los endpoints que simplemente
 * listan los bloques de una página: {@code /api/groups},
 * {@code /api/home/otros-servicios}, {@code /api/contacto},
 * {@code /api/horarios} y {@code /api/construye}.</p>
 *
 * @param title    Título del bloque (primer segmento tras la división title/subtitle).
 * @param subtitle Subtítulo del bloque (puede ser null).
 * @param text     Contenido del bloque en HTML sanitizado con Safelist.relaxed (puede ser null).
 * @param image    URL absoluta de la imagen del bloque (puede ser null).
 */
public record PageSectionDTO(
        String title,
        String subtitle,
        String text,
        String image
) {
}
