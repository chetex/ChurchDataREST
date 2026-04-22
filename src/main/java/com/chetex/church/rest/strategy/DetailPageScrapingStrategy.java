package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.DetailPageDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extrae los campos del detalle de un post (Colibri WP):
 * título, subtítulo opcional, imagen principal, fecha y contenido en texto plano.
 *
 * <p>La app móvil renderiza el texto directamente, así que aquí sólo devolvemos
 * el texto del cuerpo del artículo — párrafos unidos por doble salto de línea
 * para preservar la separación visual.</p>
 */
@Component
public class DetailPageScrapingStrategy implements ScrapingStrategy<DetailPageDTO> {

    private static final Logger log = LoggerFactory.getLogger(DetailPageScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.DETAIL;
    }

    @Override
    public DetailPageDTO extract(Document document) {
        DetailPageDTO detailPage = new DetailPageDTO(); // DTO que se irá rellenando campo a campo.

        // --- Título -------------------------------------------------------
        // Selectores típicos de Colibri + WordPress; el primero que matchee gana.
        Element titleElement = document.selectFirst("h1.entry-title, h1.post-title, h1.page-title, .h-blog-title, h1");
        if (titleElement != null) {
            detailPage.setTitle(titleElement.text()); // Texto plano del título.
        } else {
            log.debug("No title found for detail page.");
        }

        // --- Subtítulo / meta --------------------------------------------
        Element subtitleElement = document.selectFirst("p.subtitle, h2.entry-subtitle, .post-excerpt");
        if (subtitleElement != null) {
            detailPage.setSubtitle(subtitleElement.text()); // Texto plano del subtítulo.
        } else {
            log.debug("No subtitle found for detail page.");
        }

        // --- Imagen principal --------------------------------------------
        // Preferimos imágenes dentro del contenido del post; si no existen, caemos a la thumbnail Colibri.
        Element imageElement = document.selectFirst(
                ".colibri-post-content img, .entry-content img, .post-content img, .colibri-post-thumbnail img");
        if (imageElement != null) {
            String src = imageElement.attr("abs:src");
            if (src == null || src.isBlank()) src = imageElement.attr("abs:data-src"); // Lazy-loading de Colibri.
            detailPage.setImageUrl(src); // URL absoluta de la imagen.
        } else {
            log.debug("No main image found for detail page.");
        }

        // --- Fecha de publicación ----------------------------------------
        // Colibri renderiza la fecha dentro de .h-blog-meta > .metadata-item > a (p. ej. "junio 21, 2025").
        Element dateElement = document.selectFirst(".h-blog-meta .metadata-item a, .h-blog-meta .metadata-item");
        if (dateElement != null) {
            String dateText = dateElement.text();
            if (dateText != null && !dateText.isBlank()) {
                detailPage.setDate(dateText.trim()); // Fecha tal cual aparece en la web.
            }
        } else {
            log.debug("No publication date found for detail page.");
        }

        // --- Contenido en texto plano ------------------------------------
        // Contenedor principal de Colibri para el cuerpo del post.
        Element contentRoot = document.selectFirst(".colibri-post-content, .entry-content, .post-content");
        if (contentRoot != null) {
            detailPage.setContent(extractPlainText(contentRoot));
        } else {
            log.warn("No main content container found for detail page. Leaving content empty.");
            detailPage.setContent(""); // Respuesta determinista: cadena vacía en lugar de null.
        }

        return detailPage;
    }

    /**
     * Recorre los bloques de texto relevantes del contenedor (párrafos,
     * encabezados, ítems de lista y citas) y devuelve su texto concatenado
     * con doble salto de línea. Evita incluir nodos de script/estilo y
     * limpia espacios redundantes.
     */
    private String extractPlainText(Element root) {
        StringBuilder sb = new StringBuilder();                                 // Acumulador eficiente.

        // Selector ordenado: recogemos los bloques en el orden en que aparecen en el DOM.
        Elements blocks = root.select("p, h1, h2, h3, h4, h5, h6, li, blockquote");

        for (Element block : blocks) {
            String text = block.text();                                          // text() ya colapsa espacios.
            if (text == null) continue;                                          // Defensivo.
            String trimmed = text.trim();
            if (trimmed.isEmpty()) continue;                                     // Ignoramos bloques vacíos.

            if (sb.length() > 0) sb.append("\n\n");                              // Separador entre párrafos.
            sb.append(trimmed);                                                  // Añadimos el texto del bloque.
        }

        return sb.toString();                                                    // Texto final listo para la app móvil.
    }
}
