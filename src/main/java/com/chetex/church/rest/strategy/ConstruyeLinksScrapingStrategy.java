package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.ConstruyePageDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Recoge los enlaces destacados de la página "Juntos Crecemos Mejor":
 * <ul>
 *   <li>Suscripción online (formulario web).</li>
 *   <li>Suscripción PDF (ficha descargable).</li>
 *   <li>Beneficios fiscales (página interna).</li>
 * </ul>
 *
 * <p>Detecta los enlaces combinando texto del anchor y patrones de href:
 * un anchor cuyo texto contenga "suscripción online" o cuyo href apunte
 * a {@code /suscripciones-web/} es el formulario; un anchor que apunte a
 * un {@code .pdf} con el nombre "ficha-SUSCRIPCION" es la versión PDF;
 * un anchor con "beneficios fiscales" o href hacia {@code /beneficios-fiscales/}
 * es el enlace de beneficios.</p>
 *
 * <p>La estrategia devuelve un {@link ConstruyePageDTO} con solo los tres
 * links rellenos; el servicio combinará esta salida con las secciones
 * extraídas por {@link PageSectionsScrapingStrategy}.</p>
 */
@Component
public class ConstruyeLinksScrapingStrategy implements ScrapingStrategy<ConstruyePageDTO> {

    private static final Logger log = LoggerFactory.getLogger(ConstruyeLinksScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.CONSTRUYE_LINKS;
    }

    @Override
    public ConstruyePageDTO extract(Document document) {
        Element main = document.selectFirst("main");
        if (main == null) main = document.body();
        Element h1 = main.selectFirst("h1");
        String title = h1 != null ? h1.text() : null;

        String linkSuscripcionOnline = null;
        String linkSuscripcionPDF = null;
        String linkBeneficios = null;

        Elements anchors = main.select("a[href]");
        for (Element a : anchors) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) continue;
            String hrefLower = href.toLowerCase(Locale.ROOT);
            String textLower = (a.text() == null ? "" : a.text()).toLowerCase(Locale.ROOT);
            String combined = hrefLower + " " + textLower;

            // PDF suscripción: enlace a archivo .pdf con nombre "suscripcion".
            if (linkSuscripcionPDF == null && hrefLower.endsWith(".pdf") &&
                    (hrefLower.contains("suscrip") || textLower.contains("pdf"))) {
                linkSuscripcionPDF = href;
                continue;
            }

            // Suscripción online: URL de formulario web de suscripción.
            if (linkSuscripcionOnline == null &&
                    (hrefLower.contains("suscripciones-web") ||
                            (textLower.contains("suscrip") && textLower.contains("online")))) {
                linkSuscripcionOnline = href;
                continue;
            }

            // Beneficios fiscales.
            if (linkBeneficios == null && combined.contains("benefici")) {
                linkBeneficios = href;
            }
        }

        log.info("ConstruyeLinks: online={} pdf={} beneficios={}",
                linkSuscripcionOnline, linkSuscripcionPDF, linkBeneficios);
        return new ConstruyePageDTO(title, List.of(), linkSuscripcionOnline, linkSuscripcionPDF, linkBeneficios);
    }
}
