package com.chetex.church.rest.dto;

/**
 * Bloque individual de contenido extraído de una página genérica.
 *
 * <p>La app móvil renderiza nativamente el HTML contenido en {@code html},
 * así que preservamos las etiquetas originales (p, strong, em, a, ul, li,
 * h1..h6, img, figure…). El campo {@code type} es una pista semántica para
 * que la UI pueda aplicar estilos (encabezado, párrafo, imagen, lista,
 * cita, etc.) sin tener que reanalizar el HTML.</p>
 *
 * @param type  Tipo semántico del bloque: "heading", "paragraph", "image",
 *              "list", "quote", "link", "html".
 * @param html  HTML original del bloque, listo para renderizar tal cual.
 * @param text  Texto plano del bloque (útil para preview, búsqueda, a11y).
 * @param level Nivel del heading (1–6) si {@code type == "heading"}; null en los demás.
 * @param src   URL absoluta de la imagen si {@code type == "image"}; null en los demás.
 * @param alt   Texto alternativo (alt) de la imagen; null si no aplica.
 * @param href  URL absoluta si el bloque es un enlace o imagen con enlace; null en los demás.
 */
public record ContentBlockDTO(
        String type,
        String html,
        String text,
        Integer level,
        String src,
        String alt,
        String href
) {
}
