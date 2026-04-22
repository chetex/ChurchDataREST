package com.chetex.church.rest.service;

import com.chetex.church.rest.dto.*;
import com.chetex.church.rest.strategy.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Orquesta la apertura del documento HTML con Jsoup y delega la extracción
 * en la estrategia adecuada, resuelta a través de {@link ScrapingStrategyFactory}.
 * Este servicio NO contiene selectores Jsoup: toda la lógica de scraping vive
 * en las clases del paquete {@code strategy}.
 */
@Service
public class WebScrapingService {
    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int PARALLEL_WORKERS = 8;

    @Value("${church.url}")
    private String churchUrl;

    // Factory que resuelve la ScrapingStrategy adecuada por tipo.
    private final ScrapingStrategyFactory strategyFactory;

    // Fachada genérica de caché JSON en PostgreSQL: getOrRefresh + invalidate.
    private final CacheService cacheService;

    // Claves lógicas usadas para indexar las respuestas cacheadas en la tabla cached_response.
    private static final String CACHE_KEY_NAVIGATION = "home.menu";
    private static final String CACHE_KEY_HOME = "home.elements";
    private static final String CACHE_KEY_SOCIALS = "socials";

    public WebScrapingService(ScrapingStrategyFactory strategyFactory, CacheService cacheService) {
        this.strategyFactory = strategyFactory; // Inyección de factory de estrategias.
        this.cacheService = cacheService; // Inyección de la fachada de caché JSON.
    }

    public List<NavigationItemDTO> getNavigation() throws IOException {
        log.info("Solicitud de menú (cache key='{}')", CACHE_KEY_NAVIGATION);
        // getOrRefresh: devuelve la versión cacheada si existe; si no, scrapea y almacena.
        return cacheService.getOrRefresh(
                CACHE_KEY_NAVIGATION,
                new TypeReference<List<NavigationItemDTO>>() {},
                () -> {
                    log.info("Scrapeando menú desde: {}", churchUrl);
                    return executeStrategy(churchUrl, strategyFactory.get(ScrapingType.NAVIGATION));
                }
        );
    }

    /**
     * Scrape principal home and all sub-pages accessible through the menu (menus and submenus, recursively),
     * @return Aggregated list of content items.
     * @throws IOException if scraping the main home page fails.
     */
    public List<HomeContentItemDTO> getHomeAggregate() throws IOException {
        // Envolvemos el agregado con caché JSON: si hay fila en "home.elements", devolvemos sin scrape.
        return cacheService.getOrRefresh(
                CACHE_KEY_HOME,
                new TypeReference<List<HomeContentItemDTO>>() {},
                this::scrapeHomeAggregate
        );
    }

    /**
     * Implementación real del scraping agregado (BFS por oleadas con pool
     * de hilos). Se invoca únicamente cuando la caché está vacía.
     */
    private List<HomeContentItemDTO> scrapeHomeAggregate() throws IOException {
        ScrapingStrategy<List<HomeContentItemDTO>> homeStrategy = strategyFactory.get(ScrapingType.HOME);

        // Estructuras thread-safe: varios hilos escribirán en ellas a la vez.
        List<HomeContentItemDTO> aggregated = Collections.synchronizedList(new ArrayList<>());
        Set<String> visited = ConcurrentHashMap.newKeySet();

        // Semillas iniciales: home + todas las URLs del menú (aplanadas recursivamente).
        log.info("Scrapeando home principal: {}", churchUrl);
        Set<String> currentWave = new HashSet<>();
        currentWave.add(churchUrl);
        List<String> menuUrls = flattenNavigationUrls(getNavigation());
        log.info("Se añaden {} URLs del menú a la cola inicial", menuUrls.size());
        currentWave.addAll(menuUrls);

        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_WORKERS);
        long startNanos = System.nanoTime();
        try {
            // BFS por oleadas: cada iteración descarga en paralelo todas las URLs
            // conocidas en ese momento, y recolecta las follow-up (paginación)
            // que aparecerán como nueva oleada. Se repite hasta que no haya nuevas URLs.
            while (!currentWave.isEmpty()) {
                List<String> toFetch = currentWave.stream()
                        .map(this::normalizeUrl)
                        .filter(Objects::nonNull)
                        .filter(visited::add)            // add() devuelve false si ya existía → filtra ya visitadas
                        .toList();

                if (toFetch.isEmpty()) break;
                log.info("Oleada: descargando {} páginas en paralelo ({} hilos)", toFetch.size(), PARALLEL_WORKERS);

                // Enviamos cada descarga como tarea y recibimos las URLs follow-up.
                List<Future<List<String>>> futures = new ArrayList<>(toFetch.size());
                for (String url : toFetch) {
                    futures.add(executor.submit(() -> fetchAndExtract(url, homeStrategy, aggregated)));
                }

                // Recogemos resultados de la oleada actual y preparamos la siguiente.
                Set<String> nextWave = new HashSet<>();
                for (Future<List<String>> future : futures) {
                    try {
                        nextWave.addAll(future.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Tarea interrumpida durante el agregado");
                    } catch (ExecutionException e) {
                        log.warn("Tarea de scraping falló: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    }
                }
                currentWave = nextWave;
            }
        } finally {
            executor.shutdown();
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("Agregación completa: {} elementos totales desde {} páginas en {} ms",
                aggregated.size(), visited.size(), elapsedMs);
        return new ArrayList<>(aggregated);
    }

    /**
     * Descarga una página, ejecuta la estrategia y añade los items al
     * agregado compartido. Devuelve las URLs de seguimiento (paginación)
     * descubiertas, para que el orquestador las encole en la siguiente oleada.
     * Se ejecuta en un hilo worker del pool.
     */
    private List<String> fetchAndExtract(String url,
                                         ScrapingStrategy<List<HomeContentItemDTO>> strategy,
                                         List<HomeContentItemDTO> aggregated) {
        try {
            log.debug("[{}] Scrapeando: {}", Thread.currentThread().getName(), url);
            Document document = Jsoup.connect(url).userAgent(USER_AGENT).get();
            aggregated.addAll(strategy.extract(document));
            return strategy.discoverFollowUpUrls(document);
        } catch (IOException e) {
            log.warn("Fallo al scrapear {} — se omite. Causa: {}", url, e.getMessage());
            return List.of();
        }
    }

    /**
     * Normaliza una URL para comparación: quita el fragmento ({@code #...}) y
     * los espacios en blanco. Devuelve {@code null} si la URL queda vacía
     * (p. ej. si era solo {@code "#"}).
     */
    private String normalizeUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        int hashIndex = trimmed.indexOf('#');
        String withoutFragment = hashIndex >= 0 ? trimmed.substring(0, hashIndex) : trimmed;
        return withoutFragment.isEmpty() ? null : withoutFragment;
    }

    /**
     * Aplana el árbol de navegación (menús + submenús) en una lista plana de URLs.
     */
    private List<String> flattenNavigationUrls(List<NavigationItemDTO> items) {
        List<String> urls = new ArrayList<>();
        for (NavigationItemDTO item : items) {
            if (item.getUrl() != null && !item.getUrl().isBlank()) {
                urls.add(item.getUrl());
            }
            if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
                urls.addAll(flattenNavigationUrls(item.getSubItems()));
            }
        }
        return urls;
    }

    public DetailPageDTO getDetailPage(String url) throws IOException {
        return executeStrategy(url, strategyFactory.get(ScrapingType.DETAIL));
    }

    /**
     * Devuelve los enlaces a redes sociales consolidados desde header + footer,
     * con caché bajo la clave {@code "socials"}.
     */
    public List<SocialLinkDTO> getSocials() throws IOException {
        return cacheService.getOrRefresh(
                CACHE_KEY_SOCIALS,
                new TypeReference<List<SocialLinkDTO>>() {},
                () -> {
                    log.info("Scrapeando redes sociales desde: {}", churchUrl);
                    return executeStrategy(churchUrl, strategyFactory.get(ScrapingType.SOCIALS));
                }
        );
    }

    public <T> T executeStrategy(String url, ScrapingStrategy<T> strategy) throws IOException {
        Document document = Jsoup.connect(url).userAgent(USER_AGENT).get();
        return strategy.extract(document);
    }
}
