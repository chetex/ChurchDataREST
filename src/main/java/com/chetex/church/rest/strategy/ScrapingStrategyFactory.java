package com.chetex.church.rest.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory que resuelve la {@link ScrapingStrategy} adecuada para un
 * {@link ScrapingType}. Spring inyecta automáticamente todos los beans
 * que implementen la interfaz, y los indexamos por su tipo en el
 * constructor para ofrecer una resolución O(1) en tiempo de ejecución.
 *
 * Añadir una nueva estrategia consiste únicamente en crear un nuevo
 * {@code @Component} que implemente {@link ScrapingStrategy} y declarar
 * su tipo en {@link ScrapingType}; esta factory la recogerá sin
 * modificaciones (Open/Closed).
 */
@Component
public class ScrapingStrategyFactory {

    private final Map<ScrapingType, ScrapingStrategy<?>> strategiesByType;

    public ScrapingStrategyFactory(List<ScrapingStrategy<?>> strategies) {
        this.strategiesByType = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(ScrapingStrategy::getType, Function.identity()));
    }

    /**
     * Devuelve la estrategia registrada para el tipo dado.
     *
     * @param type Tipo de scraping solicitado.
     * @param <T>  Tipo del DTO devuelto por la estrategia (responsabilidad del llamador).
     * @throws IllegalArgumentException si no hay ninguna estrategia registrada para ese tipo.
     */
    @SuppressWarnings("unchecked")
    public <T> ScrapingStrategy<T> get(ScrapingType type) {
        ScrapingStrategy<?> strategy = strategiesByType.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No hay estrategia registrada para el tipo: " + type);
        }
        return (ScrapingStrategy<T>) strategy;
    }
}
