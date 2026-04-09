package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.HomeContentItemDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estrategia que extrae bloques de contenido de las páginas del sitio
 * (construido con <b>Colibri WP</b>) y los mapea a {@link HomeContentItemDTO}.
 *
 * <p>El sitio usa dos estructuras distintas:
 * <ul>
 *   <li><b>Listados de posts</b> (home, categorías): cada post vive en un
 *       contenedor con clase {@code .hentry} y contiene {@code .h-blog-title},
 *       {@code .colibri-post-excerpt} y {@code .colibri-post-thumbnail img}.</li>
 *   <li><b>Páginas estáticas</b> (horario, contact, enlaces…): no tienen
 *       {@code .hentry}; el contenido se compone de {@code .h-heading} y
 *       bloques de texto sueltos. Para estas páginas se aplica un fallback
 *       que agrupa cada heading con el texto que le sigue.</li>
 * </ul>
 */
@Component
public class HomeContentScrapingStrategy implements ScrapingStrategy<List<HomeContentItemDTO>> {

    private static final Logger log = LoggerFactory.getLogger(HomeContentScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.HOME;
    }

    @Override
    public List<HomeContentItemDTO> extract(Document document) {
        String sourceUrl = document.location();
        List<HomeContentItemDTO> items = new ArrayList<>();

        // --- Ruta 1: listados de posts (Colibri WP) ---
        Elements posts = document.select(".hentry");
        if (!posts.isEmpty()) {
            log.debug("Detectados {} posts Colibri en {}", posts.size(), sourceUrl);
            for (Element post : posts) {
                HomeContentItemDTO item = buildFromColibriPost(post, sourceUrl);
                if (item != null) items.add(item);
            }
        }

        // --- Ruta 2: fallback para páginas estáticas (sin .hentry o con pocos hits) ---
        if (items.isEmpty()) {
            log.debug("Sin posts Colibri en {}, aplicando fallback de página estática", sourceUrl);
            items.addAll(buildFromStaticPage(document, sourceUrl));
        }

        log.info("Extraídos {} elementos de contenido desde {}", items.size(), sourceUrl);
        return items;
    }

    // ---------------------------------------------------------------------
    // Ruta 1: posts de Colibri WP
    // ---------------------------------------------------------------------

    private HomeContentItemDTO buildFromColibriPost(Element post, String sourceUrl) {
        Element titleEl = post.selectFirst(".h-blog-title");
        Element titleAnchor = titleEl != null ? titleEl.selectFirst("a") : null;

        String title = titleEl != null ? titleEl.text() : null;
        String linkUrl = titleAnchor != null ? titleAnchor.attr("abs:href") : null;
        String text = textOrNull(post.selectFirst(".colibri-post-excerpt"));
        String image = firstImageAbsSrc(post.selectFirst(".colibri-post-thumbnail"));
        // Fecha: Colibri la renderiza dentro de .h-blog-meta > .metadata-item (p. ej. "marzo 29, 2026").
        String date = textOrNull(post.selectFirst(".h-blog-meta .metadata-item"));

        if (isBlank(title) && isBlank(text)) return null;

        return new HomeContentItemDTO(
                safeTrim(title),
                safeTrim(text),
                nullIfBlank(image),
                nullIfBlank(linkUrl),
                nullIfBlank(date),
                sourceUrl
        );
    }

    /**
     * Descubre enlaces de paginación (WordPress estándar: {@code a.page-numbers})
     * para que el servicio pueda seguir recorriendo listados multipágina.
     */
    @Override
    public List<String> discoverFollowUpUrls(Document document) {
        List<String> urls = new ArrayList<>();
        for (Element a : document.select("a.page-numbers[href]")) {
            String href = a.attr("abs:href");
            if (!isBlank(href) && !"#".equals(href)) {
                urls.add(href);
            }
        }
        return urls;
    }

    // ---------------------------------------------------------------------
    // Ruta 2: páginas estáticas (fallback basado en headings + texto)
    // ---------------------------------------------------------------------

    private List<HomeContentItemDTO> buildFromStaticPage(Document document, String sourceUrl) {
        List<HomeContentItemDTO> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Buscamos headings relevantes: los de Colibri y los nativos.
        Elements headings = document.select(".h-heading, h1, h2, h3");

        for (Element heading : headings) {
            String title = heading.text();
            if (isBlank(title) || title.length() > 200) continue;

            // Subimos al contenedor "h-element" más cercano para agrupar heading + texto + imagen.
            Element container = heading.closest(".h-element, .h-x-container, section, div");
            if (container == null) container = heading.parent();
            if (container == null) continue;

            String text = extractFollowingText(container, heading);
            String image = firstImageAbsSrc(container);
            String linkUrl = firstAnchorAbsHref(container);

            // Descartar si solo hay título sin ningún texto ni imagen: son probablemente headings de layout.
            if (isBlank(text) && isBlank(image)) continue;

            String fingerprint = (title + "|" + (text == null ? "" : text)).toLowerCase();
            if (!seen.add(fingerprint)) continue;

            items.add(new HomeContentItemDTO(
                    safeTrim(title),
                    safeTrim(text),
                    nullIfBlank(image),
                    nullIfBlank(linkUrl),
                    null, // las páginas estáticas no tienen fecha de publicación
                    sourceUrl
            ));
        }

        return items;
    }

    /**
     * Devuelve el texto del contenedor excluyendo el propio heading,
     * para no duplicar el título en el campo {@code text}.
     */
    private String extractFollowingText(Element container, Element headingToExclude) {
        StringBuilder sb = new StringBuilder();
        for (Element p : container.select("p, .colibri-word-wrap, li")) {
            if (p.equals(headingToExclude) || headingToExclude.equals(p.parent())) continue;
            String t = p.text();
            if (!isBlank(t)) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(t);
            }
            if (sb.length() > 500) break; // corte razonable
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String textOrNull(Element el) {
        if (el == null) return null;
        String t = el.text();
        return isBlank(t) ? null : t;
    }

    private String firstImageAbsSrc(Element root) {
        if (root == null) return null;
        Element img = root.selectFirst("img");
        if (img == null) return null;
        String src = img.attr("abs:src");
        if (isBlank(src)) src = img.attr("abs:data-src"); // lazy-loading
        return isBlank(src) ? null : src;
    }

    private String firstAnchorAbsHref(Element root) {
        if (root == null) return null;
        Element a = root.selectFirst("a[href]");
        if (a == null) return null;
        String href = a.attr("abs:href");
        return isBlank(href) || "#".equals(href) ? null : href;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private String nullIfBlank(String s) {
        return isBlank(s) ? null : s;
    }
}
