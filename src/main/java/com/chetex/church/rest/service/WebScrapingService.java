package com.chetex.church.rest.service;

import com.chetex.church.rest.dto.DetailPageDTO;
import com.chetex.church.rest.dto.HomeContentItemDTO;
import com.chetex.church.rest.dto.NavigationItemDTO;
import com.chetex.church.rest.dto.NewsItemDTO;
import com.chetex.church.rest.strategy.ScrapingStrategy;
import com.chetex.church.rest.strategy.ScrapingStrategyFactory;
import com.chetex.church.rest.strategy.ScrapingType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Value("${church.url}")
    private String churchUrl;

    private final ScrapingStrategyFactory strategyFactory;

    public WebScrapingService(ScrapingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public List<NavigationItemDTO> getNavigation() throws IOException {
        log.info("Obteniendo navegación desde: {}", churchUrl);
        return executeStrategy(churchUrl, strategyFactory.get(ScrapingType.NAVIGATION));
    }

    public List<NewsItemDTO> getHomeNews() throws IOException {
        ScrapingStrategy<List<NewsItemDTO>> newsStrategy = strategyFactory.get(ScrapingType.NEWS);

        // 1. Intentar localizar una sección de noticias dentro del menú principal.
        Optional<String> newsPageUrl = getNavigation().stream()
                .filter(item -> {
                    String name = item.getName().toLowerCase();
                    return name.contains("actualidad") || name.contains("noticias") || name.contains("blog");
                })
                .map(NavigationItemDTO::getUrl)
                .findFirst();

        if (newsPageUrl.isPresent()) {
            log.info("Página específica de noticias encontrada: {}", newsPageUrl.get());
            List<NewsItemDTO> items = executeStrategy(newsPageUrl.get(), newsStrategy);
            if (!items.isEmpty()) return items;
        }

        // 2. Fallback: scrapear noticias directamente desde la home.
        log.info("Scrapeando noticias directamente desde la home: {}", churchUrl);
        return executeStrategy(churchUrl, newsStrategy);
    }

    /**
     * Agrega el contenido de la home principal, de todas las sub-páginas
     * alcanzables a través del menú (menús + submenús) y de la paginación
     * descubierta dinámicamente (p. ej. {@code /page/2/}, {@code /page/3/}…).
     *
     * <p>Usa una búsqueda en anchura (BFS) con cola de URLs pendientes y
     * un set de URLs ya visitadas. Las URLs se normalizan (se les quita el
     * fragmento {@code #...}) antes de comparar, para evitar duplicados
     * como {@code https://sitio/} vs {@code https://sitio/#}.</p>
     *
     * <p>Cada página se procesa:
     * <ol>
     *   <li>Se scrapea su contenido con la estrategia {@code HOME}.</li>
     *   <li>Se le pide a la estrategia descubrir URLs de seguimiento
     *       (paginación) y se encolan si son nuevas.</li>
     * </ol>
     * Los fallos por página se registran en log pero no abortan el agregado.</p>
     */
    /** Número de hilos concurrentes para descargar páginas. IO-bound: no escala con CPU sino con red. */
    private static final int PARALLEL_WORKERS = 8;

    public List<HomeContentItemDTO> getHomeAggregate() throws IOException {
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

    public <T> T executeStrategy(String url, ScrapingStrategy<T> strategy) throws IOException {
        Document document = Jsoup.connect(url).userAgent(USER_AGENT).get();
        return strategy.extract(document);
    }
}
