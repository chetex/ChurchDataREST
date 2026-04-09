package com.chetex.church.rest.strategy;

import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Strategy interface to define different types of data extraction (web scraping).
 * @param <T> The type of data object resulting from the extraction.
 */
public interface ScrapingStrategy<T> {
    /**
     * Extracts specific data from a Jsoup HTML document.
     * @param document The HTML document to process.
     * @return The extracted data object.
     */
    T extract(Document document);

    /**
     * Tipo de scraping que implementa esta estrategia. Lo usa la
     * {@link ScrapingStrategyFactory} para indexar los beans.
     */
    ScrapingType getType();

    /**
     * Descubre URLs adicionales relacionadas (paginación, "siguientes", etc.)
     * que deberían visitarse tras procesar el documento. Por defecto, vacío.
     * Las estrategias que manejan listados paginados pueden sobrescribirlo.
     */
    default List<String> discoverFollowUpUrls(Document document) {
        return List.of();
    }
}
