package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.HorarioPageDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Recoge los enlaces destacados de la página de horarios:
 * <ul>
 *   <li>Horario de Verano.</li>
 *   <li>Horario de Invierno (si existe en la web).</li>
 *   <li>Horario Misas en todo Tres Cantos.</li>
 * </ul>
 *
 * <p>Devuelve un {@link HorarioPageDTO} con solo los tres enlaces rellenos
 * y {@code title} nulo / {@code sections} vacía; el servicio combinará
 * esta salida con la de {@link PageSectionsScrapingStrategy} para
 * construir la respuesta final del endpoint {@code /api/horarios}.</p>
 */
@Component
public class HorarioLinksScrapingStrategy implements ScrapingStrategy<HorarioPageDTO> {

    private static final Logger log = LoggerFactory.getLogger(HorarioLinksScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.HORARIO_LINKS;
    }

    @Override
    public HorarioPageDTO extract(Document document) {
        // Título principal (h1 del <main>).
        Element main = document.selectFirst("main");
        if (main == null) main = document.body();
        Element h1 = main.selectFirst("h1");
        String title = h1 != null ? h1.text() : null;

        // Escaneamos todos los anchors del main buscando palabras clave.
        String linkVerano = null;
        String linkInvierno = null;
        String linkTresCantos = null;

        Elements anchors = main.select("a[href]");
        for (Element a : anchors) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) continue;
            String text = a.text() == null ? "" : a.text().toLowerCase(Locale.ROOT);
            if (text.isBlank()) continue;

            if (linkVerano == null && text.contains("verano")) {
                linkVerano = href;
            } else if (linkInvierno == null && text.contains("invierno")) {
                linkInvierno = href;
            } else if (linkTresCantos == null && (text.contains("tres cantos") || text.contains("todo tres"))) {
                linkTresCantos = href;
            }
        }

        log.info("HorarioLinks: verano={} invierno={} tresCantos={}", linkVerano, linkInvierno, linkTresCantos);
        return new HorarioPageDTO(title, linkVerano, linkInvierno, linkTresCantos, List.of());
    }
}
