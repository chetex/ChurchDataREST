package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.PageSectionDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estrategia genérica para páginas estáticas (grupos, contacto, horario,
 * otros-servicios, juntos-crecemos-mejor...) que no siguen el patrón de
 * posts {@code .hentry}.
 *
 * <p>El tema Colibri WP organiza el contenido en columnas
 * ({@code .h-column}) donde normalmente conviven un heading ({@code .h-heading}),
 * una imagen ({@code .h-image} o {@code img}) y un bloque de texto enriquecido
 * ({@code .h-text-component}). Este scraper recorre cada heading visible del
 * {@code <main>} y agrupa heading + imagen + texto del contenedor más
 * cercano, devolviendo una lista de {@link PageSectionDTO}.</p>
 *
 * <p>Excluye explícitamente las cabeceras del popup de newsletter
 * ({@code .ays_pb_*}) y los headings con {@code display:none} inline que el
 * tema pinta como placeholders ocultos.</p>
 */
@Component
public class PageSectionsScrapingStrategy implements ScrapingStrategy<List<PageSectionDTO>> {

    private static final Logger log = LoggerFactory.getLogger(PageSectionsScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.PAGE_SECTIONS;
    }

    @Override
    public List<PageSectionDTO> extract(Document document) {
        // 1. Acotamos al <main> de la página: descarta cabecera global, footer y popups ays_*.
        Element main = document.selectFirst("main");
        if (main == null) main = document.body();

        List<PageSectionDTO> sections = new ArrayList<>();
        Set<String> fingerprints = new HashSet<>();

        // 2. Iteramos los headings semánticos (h1-h4) dentro del main.
        Elements headings = main.select("h1, h2, h3, h4");
        for (Element h : headings) {
            // a) Excluye headings dentro del popup de suscripción (ays_pb_*) y otros popups.
            if (h.closest("[class*=ays_pb], [class*=ays_popup], [id*=ays_pb], [class*=popup-inner]") != null) continue;

            // b) Excluye headings ocultos con display:none inline.
            String style = h.attr("style");
            if (style != null && style.replace(" ", "").toLowerCase().contains("display:none")) continue;

            // c) Descarta títulos vacíos o absurdamente largos.
            String rawTitle = h.text();
            if (rawTitle == null || rawTitle.isBlank() || rawTitle.length() > 200) continue;

            // d) Normaliza el título y separa en title/subtitle por delimitadores comunes.
            String[] split = splitTitleAndSubtitle(rawTitle);

            // e) Sube al contenedor columna/fila más cercano para agrupar el bloque.
            Element container = h.closest(".h-column, .h-row, .h-element, section, article, div");
            if (container == null) container = h.parent();
            if (container == null) continue;

            // f) Extrae la primera imagen del contenedor (abs:src o lazy abs:data-src).
            String image = firstImageAbsSrc(container, document.baseUri());

            // g) Texto en HTML sanitizado: reúne el h-text-component o párrafos del contenedor.
            String html = extractSanitizedHtml(container, h, document.baseUri());

            // h) Si solo tenemos un título sin texto ni imagen, es probable maquetación: descartamos.
            if ((html == null || html.isBlank()) && (image == null || image.isBlank())) continue;

            // i) Dedupe por título+texto (mismo bloque repetido en distintos niveles de anidado).
            String fp = (split[0] + "|" + (html == null ? "" : html.length())).toLowerCase();
            if (!fingerprints.add(fp)) continue;

            sections.add(new PageSectionDTO(
                    nullIfBlank(split[0]),
                    nullIfBlank(split[1]),
                    nullIfBlank(html),
                    nullIfBlank(image)
            ));
        }

        log.info("PageSections: extraídos {} bloques desde {}", sections.size(), document.location());
        return sections;
    }

    /**
     * Recoge la primera imagen del contenedor resolviendo a URL absoluta.
     * Descarta logos globales (img dentro de header/nav) si los selecciona
     * accidentalmente.
     */
    private String firstImageAbsSrc(Element container, String baseUri) {
        Element img = container.selectFirst("img");
        if (img == null) return null;
        String src = img.attr("abs:src");
        if (src == null || src.isBlank()) src = img.attr("abs:data-src");
        return (src == null || src.isBlank()) ? null : src;
    }

    /**
     * Devuelve el HTML sanitizado asociado a un heading dentro de su
     * contenedor. Se excluye el propio heading para no duplicar el título
     * en el cuerpo, y se resuelven los href/src a URLs absolutas antes
     * de pasar por el cleaner.
     */
    private String extractSanitizedHtml(Element container, Element headingToExclude, String baseUri) {
        // Buscamos bloques de texto enriquecido típicos de Colibri + HTML nativo.
        Elements textBlocks = container.select(".h-text-component, .h-text, p, ul, ol, blockquote");
        if (textBlocks.isEmpty()) return null;

        StringBuilder raw = new StringBuilder();
        for (Element block : textBlocks) {
            // No metemos el heading dentro del cuerpo.
            if (block.equals(headingToExclude) || headingToExclude.equals(block.parent())) continue;
            // Skip el popup de suscripción si fuera descendiente del contenedor.
            if (block.closest("[class*=ays_pb], [class*=popup-inner]") != null) continue;

            // Resolvemos enlaces e imágenes a URLs absolutas antes de serializar.
            for (Element a : block.select("a[href]")) {
                String abs = a.attr("abs:href");
                if (abs != null && !abs.isBlank()) a.attr("href", abs);
            }
            for (Element img : block.select("img")) {
                String abs = img.attr("abs:src");
                if (abs == null || abs.isBlank()) abs = img.attr("abs:data-src");
                if (abs != null && !abs.isBlank()) img.attr("src", abs);
            }

            String blockHtml = block.outerHtml();
            if (blockHtml != null && !blockHtml.isBlank()) {
                if (raw.length() > 0) raw.append("\n");
                raw.append(blockHtml);
            }
            if (raw.length() > 10_000) break; // corte razonable
        }

        if (raw.length() == 0) return null;

        Safelist safelist = Safelist.relaxed()
                .addAttributes("a", "target", "rel")
                .addTags("br");

        return Jsoup.clean(raw.toString(), baseUri, safelist).trim();
    }

    /**
     * Divide un título bruto en (title, subtitle) usando los separadores más
     * frecuentes. Réplica del helper usado en HomeContentScrapingStrategy.
     */
    private String[] splitTitleAndSubtitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) return new String[] { null, null };
        String normalized = rawTitle.replace(' ', ' ').trim();
        String[] separators = { " — ", " – ", " - ", " — ", " – ", ": ", " | ", "\n" };
        for (String sep : separators) {
            int idx = normalized.indexOf(sep);
            if (idx > 0 && idx < normalized.length() - sep.length()) {
                String head = normalized.substring(0, idx).trim();
                String tail = normalized.substring(idx + sep.length()).trim();
                if (!head.isEmpty() && !tail.isEmpty()) return new String[] { head, tail };
            }
        }
        return new String[] { normalized, null };
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
