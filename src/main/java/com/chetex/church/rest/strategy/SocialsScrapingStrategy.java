package com.chetex.church.rest.strategy;

// DTO producido por la estrategia.
import com.chetex.church.rest.dto.SocialLinkDTO;

// Tipos Jsoup.
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Logging.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Bean Spring.
import org.springframework.stereotype.Component;

// Utilidades estándar.
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Estrategia que recopila los enlaces a redes sociales de la web.
 *
 * <p>Busca primero en el {@code <header>} (donde Colibri WP y la mayoría de
 * temas colocan los iconos sociales) y después en el {@code <footer>}, con
 * énfasis en detectar un enlace de Telegram que en este sitio suele vivir
 * solo en el pie. Los resultados se deduplican por nombre de red con el
 * criterio "el primero en aparecer gana".</p>
 */
@Component // Bean Spring registrado por la factory de estrategias.
public class SocialsScrapingStrategy implements ScrapingStrategy<List<SocialLinkDTO>> {

    // Logger operativo.
    private static final Logger log = LoggerFactory.getLogger(SocialsScrapingStrategy.class);

    // Mapa dominio → nombre canónico de red. El orden de inserción define la prioridad de detección.
    private static final Map<String, String> DOMAIN_TO_NETWORK = buildDomainMap();

    @Override
    public ScrapingType getType() {
        return ScrapingType.SOCIALS; // Identificador consumido por la factory.
    }

    @Override
    public List<SocialLinkDTO> extract(Document document) {
        // LinkedHashMap preserva orden de inserción y permite deduplicar por red.
        Map<String, SocialLinkDTO> byNetwork = new LinkedHashMap<>();

        // 1. Header: redes sociales generales del sitio.
        Element header = document.selectFirst("header");
        if (header != null) {
            collectFromScope(header, byNetwork);                                // Recolecta enlaces visibles en la cabecera.
        } else {
            log.debug("No <header> encontrado; saltamos extracción de cabecera.");
        }

        // 2. Footer: búsqueda dirigida de Telegram (único caso exclusivo del pie en este sitio).
        Element footer = document.selectFirst("footer");
        if (footer != null) {
            collectTelegramFromFooter(footer, byNetwork);                       // Inyecta telegram si no apareció antes.
            // Recolectamos también el resto por si el tema mueve iconos al footer.
            collectFromScope(footer, byNetwork);
        } else {
            log.debug("No <footer> encontrado; saltamos extracción del pie.");
        }

        List<SocialLinkDTO> result = new ArrayList<>(byNetwork.values());       // Conversión a lista ordenada.
        log.info("Redes sociales detectadas: {}", result.size());
        return result;
    }

    /**
     * Escanea todos los {@code <a href>} dentro de un ámbito (header/footer)
     * y registra el primero que matche cada red social conocida.
     */
    private void collectFromScope(Element scope, Map<String, SocialLinkDTO> byNetwork) {
        Elements anchors = scope.select("a[href]");                             // Todos los enlaces del ámbito.
        for (Element a : anchors) {
            String href = a.attr("abs:href");                                   // URL absoluta.
            if (href == null || href.isBlank()) continue;
            String network = classifyNetwork(href);                             // Nombre canónico o null.
            if (network == null) continue;
            byNetwork.putIfAbsent(network, new SocialLinkDTO(network, href));   // Primer hit gana.
        }
    }

    /**
     * Busca específicamente el enlace de Telegram en el footer. Además del
     * dominio {@code t.me}, también se aceptan {@code telegram.me} y el
     * fragmento "telegram" presente en atributos {@code aria-label}/texto.
     */
    private void collectTelegramFromFooter(Element footer, Map<String, SocialLinkDTO> byNetwork) {
        if (byNetwork.containsKey("telegram")) return;                          // Ya localizado antes; no duplicamos.
        Elements anchors = footer.select("a[href]");
        for (Element a : anchors) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) continue;
            String lower = href.toLowerCase(Locale.ROOT);
            String label = (a.attr("aria-label") + " " + a.text()).toLowerCase(Locale.ROOT);
            boolean hrefMatch = lower.contains("t.me/") || lower.contains("telegram.me/") || lower.contains("telegram.org/");
            boolean textMatch = label.contains("telegram");
            if (hrefMatch || textMatch) {
                byNetwork.putIfAbsent("telegram", new SocialLinkDTO("telegram", href));
                return;                                                          // Solo necesitamos el primer match.
            }
        }
    }

    /**
     * Clasifica la URL en una red social conocida. Devuelve null si no hay match.
     */
    private String classifyNetwork(String href) {
        String lower = href.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : DOMAIN_TO_NETWORK.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    /**
     * Construye la tabla de dominios reconocidos. Mantener orden para priorizar
     * dominios más específicos (p. ej. youtu.be antes que youtube.com).
     */
    private static Map<String, String> buildDomainMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("facebook.com", "facebook");
        m.put("fb.com", "facebook");
        m.put("instagram.com", "instagram");
        m.put("twitter.com", "twitter");
        m.put("x.com", "twitter");
        m.put("youtu.be", "youtube");
        m.put("youtube.com", "youtube");
        m.put("tiktok.com", "tiktok");
        m.put("linkedin.com", "linkedin");
        m.put("t.me", "telegram");
        m.put("telegram.me", "telegram");
        m.put("telegram.org", "telegram");
        m.put("whatsapp.com", "whatsapp");
        m.put("wa.me", "whatsapp");
        m.put("spotify.com", "spotify");
        m.put("flickr.com", "flickr");
        m.put("pinterest.com", "pinterest");
        return m;
    }
}
