package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.DetailPageDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DetailPageScrapingStrategy implements ScrapingStrategy<DetailPageDTO> {

    private static final Logger log = LoggerFactory.getLogger(DetailPageScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.DETAIL;
    }

    @Override
    public DetailPageDTO extract(Document document) {
        DetailPageDTO detailPage = new DetailPageDTO();

        // Extraer Título
        Element titleElement = document.selectFirst("h1.entry-title, h1.post-title, h1.page-title, h1");
        if (titleElement != null) {
            detailPage.setTitle(titleElement.text());
        } else {
            log.debug("No title found for detail page.");
        }

        // Extraer Subtítulo (puede ser un p, h2, o div con una clase específica)
        Element subtitleElement = document.selectFirst("p.subtitle, .entry-meta, .post-meta, h2.entry-subtitle, .post-excerpt");
        if (subtitleElement != null) {
            detailPage.setSubtitle(subtitleElement.text());
        } else {
            log.debug("No subtitle found for detail page.");
        }

        // Extraer Imagen Principal (buscar en el contenido principal o en el header)
        Element imageElement = document.selectFirst(".entry-content img, .post-content img, .main-image img, .header-image img");
        if (imageElement != null) {
            detailPage.setImageUrl(imageElement.attr("abs:src"));
        } else {
            log.debug("No main image found for detail page.");
        }

        // Extraer Contenido Completo (HTML)
        // Suponemos que el contenido principal está en un div o article con clases comunes
        Elements contentElements = document.select(".entry-content, .post-content, article.content, div.main-content");
        if (!contentElements.isEmpty()) {
            // Podríamos querer limpiar el HTML de scripts o elementos no deseados aquí
            detailPage.setFullContentHtml(contentElements.first().html());
        } else {
            log.warn("No main content found for detail page with common selectors. Consider adjusting selectors.");
            // Si no se encuentra con selectores específicos, intentar con el body o un div más genérico
            Element bodyContent = document.selectFirst("body");
            if (bodyContent != null) {
                detailPage.setFullContentHtml(bodyContent.html());
            }
        }

        return detailPage;
    }
}
