package com.chetex.church.rest.strategy;

/**
 * Tipos de scraping soportados. Cada estrategia declara el suyo
 * para que la {@link ScrapingStrategyFactory} pueda resolverla por tipo.
 */
public enum ScrapingType {
    NAVIGATION,
    NEWS,
    DETAIL,
    HOME
}
