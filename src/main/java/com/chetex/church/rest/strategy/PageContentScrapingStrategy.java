package com.chetex.church.rest.strategy;

// DTOs producidos por la estrategia.
import com.chetex.church.rest.dto.ContentBlockDTO;
import com.chetex.church.rest.dto.PageContentDTO;

// Tipos Jsoup para navegar el documento HTML.
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// SLF4J para trazabilidad.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Bean Spring.
import org.springframework.stereotype.Component;

// Utilidades estándar.
import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia que extrae el contenido principal de una página arbitraria y
 * lo entrega como una lista de {@link ContentBlockDTO}, preservando las
 * etiquetas HTML originales en cada bloque para que la app móvil las
 * renderice de forma nativa.
 *
 * <p>Identifica encabezados (h1–h6), párrafos (incluidos los que contienen
 * enlaces, negrita y énfasis), listas (ul/ol), citas (blockquote) e
 * imágenes (figure/img) situadas dentro del contenedor principal de la
 * página (tipicamente {@code <main>}, {@code <article>} o el wrapper de
 * contenido de Colibri WP).</p>
 */
@Component // Bean Spring descubierto por component-scan y registrado en la factory.
public class PageContentScrapingStrategy implements ScrapingStrategy<PageContentDTO> {

    // Logger usado para depurar selectores e incidencias en el parseo.
    private static final Logger log = LoggerFactory.getLogger(PageContentScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.PAGE_CONTENT; // Identificador usado por ScrapingStrategyFactory.
    }

    @Override
    public PageContentDTO extract(Document document) {
        String url = document.location();                                      // URL real tras redirecciones.
        String title = resolvePageTitle(document);                             // Título legible de la página.
        Element root = resolveContentRoot(document);                           // Contenedor principal o body.

        List<ContentBlockDTO> blocks = new ArrayList<>();                      // Acumulador ordenado de bloques.
        if (root == null) {                                                    // Documento vacío o sin body.
            log.warn("No content root found for {}", url);
            return new PageContentDTO(url, title, blocks);
        }

        // Selector que captura los bloques relevantes en orden de aparición.
        Elements candidates = root.select(
                "h1, h2, h3, h4, h5, h6, p, ul, ol, blockquote, figure, img"
        );

        for (Element el : candidates) {                                        // Recorremos cada bloque candidato.
            if (shouldSkip(el)) continue;                                      // Saltamos bloques ya cubiertos por un padre.

            ContentBlockDTO block = buildBlock(el);                            // Convertimos a DTO semántico.
            if (block != null) blocks.add(block);                              // Ignoramos bloques vacíos.
        }

        log.info("Extraídos {} bloques de contenido desde {}", blocks.size(), url);
        return new PageContentDTO(url, title, blocks);                         // Respuesta final agrupada.
    }

    /**
     * Resuelve el contenedor principal de contenido. Para sitios Colibri WP
     * suele ser {@code main.site-main} o {@code article}; si no existe,
     * caemos al {@code body} completo.
     */
    private Element resolveContentRoot(Document document) {
        // Intentamos contenedores específicos antes de caer al body.
        String[] candidates = {
                "main article",
                "article",
                "main.site-main",
                "main",
                ".entry-content",
                ".h-entry-content",
                "#main",
                "#content"
        };
        for (String sel : candidates) {
            Element el = document.selectFirst(sel);
            if (el != null) return el;                                         // Primer match gana.
        }
        return document.body();                                                // Último recurso: todo el body.
    }

    /**
     * Deriva un título legible para la página: primero el primer {@code h1}
     * dentro del contenido, si no existe el título del documento HTML.
     */
    private String resolvePageTitle(Document document) {
        Element h1 = document.selectFirst("main h1, article h1, .entry-title, h1");
        if (h1 != null && !h1.text().isBlank()) return h1.text().trim();
        String docTitle = document.title();                                    // {@code <title>} del HTML.
        return docTitle == null ? "" : docTitle.trim();
    }

    /**
     * Evita duplicar contenido cuando un elemento ya está contenido en otro
     * que también seleccionamos (por ejemplo, un {@code img} dentro de una
     * {@code figure}: procesamos la figure, saltamos el img).
     */
    private boolean shouldSkip(Element el) {
        // Saltar img si su ancestro más cercano figure/picture ya será procesado.
        if ("img".equalsIgnoreCase(el.tagName())) {
            Element figure = el.closest("figure, picture");
            return figure != null;                                             // Figure captura la img.
        }
        // Evitar procesar párrafos/listas vacíos.
        if ("p".equalsIgnoreCase(el.tagName()) && el.text().isBlank() && el.selectFirst("img") == null) {
            return true;
        }
        return false;
    }

    /**
     * Convierte un elemento HTML en su {@link ContentBlockDTO} correspondiente.
     * El campo {@code html} conserva el marcado original — incluidas etiquetas
     * {@code <strong>}, {@code <em>}, {@code <a>}, {@code <br>}, etc. — para
     * que la app móvil haga el render final.
     */
    private ContentBlockDTO buildBlock(Element el) {
        String tag = el.tagName().toLowerCase();                               // Nombre de etiqueta normalizado.

        switch (tag) {
            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                int level = Character.digit(tag.charAt(1), 10);                 // Extrae el nivel (1–6).
                return new ContentBlockDTO(
                        "heading",                                              // Tipo semántico.
                        el.outerHtml(),                                         // HTML completo incluida la etiqueta.
                        el.text(),                                              // Texto plano para a11y/búsqueda.
                        level,                                                  // Nivel del heading.
                        null, null, null                                        // Campos específicos de imagen/link no aplican.
                );

            case "p":
                Element anchorOnly = singleAnchorParagraph(el);                 // Detecta "p > a" como bloque de enlace.
                if (anchorOnly != null) {
                    return new ContentBlockDTO(
                            "link",                                              // Tipo "link" si el párrafo solo envuelve un anchor.
                            el.outerHtml(),
                            el.text(),
                            null, null, null,
                            anchorOnly.attr("abs:href")                          // URL absoluta del anchor.
                    );
                }
                return new ContentBlockDTO(
                        "paragraph",                                             // Párrafo genérico (con posibles hijos formateados).
                        el.outerHtml(),
                        el.text(),
                        null, null, null, null
                );

            case "ul": case "ol":
                return new ContentBlockDTO(
                        "list",                                                  // Lista ordenada o desordenada.
                        el.outerHtml(),
                        el.text(),
                        null, null, null, null
                );

            case "blockquote":
                return new ContentBlockDTO(
                        "quote",                                                 // Bloque de cita literal.
                        el.outerHtml(),
                        el.text(),
                        null, null, null, null
                );

            case "figure":
                Element img = el.selectFirst("img");                             // Imagen envuelta en figure.
                if (img == null) return null;                                   // Figure sin imagen → descartamos.
                return buildImageBlock(el, img);

            case "img":
                return buildImageBlock(el, el);

            default:
                return null;                                                    // Tipo no soportado.
        }
    }

    /**
     * Devuelve el único anchor de un párrafo cuando éste es el contenido
     * principal (el párrafo actúa como "botón/enlace"). Devuelve null en
     * caso contrario para que el bloque se procese como párrafo normal.
     */
    private Element singleAnchorParagraph(Element paragraph) {
        Elements anchors = paragraph.select("a[href]");
        if (anchors.size() != 1) return null;                                   // Múltiples anchors → párrafo normal.
        Element anchor = anchors.first();
        if (anchor == null) return null;
        String paragraphText = paragraph.text().trim();                         // Texto total del párrafo.
        String anchorText = anchor.text().trim();                               // Texto del anchor.
        // Si el texto del párrafo coincide con el del anchor, es un bloque-enlace.
        return paragraphText.equals(anchorText) ? anchor : null;
    }

    /**
     * Construye un ContentBlockDTO de tipo "image" tomando atributos del
     * propio {@code img} (src, alt) y, si existe, el anchor envolvente
     * (href) para que la app móvil pueda abrir el enlace asociado.
     */
    private ContentBlockDTO buildImageBlock(Element container, Element img) {
        String src = img.attr("abs:src");                                       // URL absoluta del src.
        if (src == null || src.isBlank()) src = img.attr("abs:data-src");       // Lazy-loading (Colibri).
        String alt = img.attr("alt");                                           // Texto alternativo.
        Element wrappingAnchor = container.closest("a[href]");                  // Enlace envolvente (figure > a > img).
        String href = wrappingAnchor != null ? wrappingAnchor.attr("abs:href") : null;

        return new ContentBlockDTO(
                "image",                                                         // Tipo semántico.
                container.outerHtml(),                                           // HTML completo (figure o img).
                alt == null ? "" : alt,                                          // Texto plano = alt (o vacío).
                null,                                                            // level no aplica a imágenes.
                src == null || src.isBlank() ? null : src,                       // Normalizamos src vacío a null.
                alt == null || alt.isBlank() ? null : alt,                       // Normalizamos alt vacío a null.
                href == null || href.isBlank() ? null : href                     // Normalizamos href vacío a null.
        );
    }
}
