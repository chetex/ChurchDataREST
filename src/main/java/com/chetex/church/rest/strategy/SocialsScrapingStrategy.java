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
 * <p>El sitio está construido con Colibri WP, que no usa la etiqueta
 * {@code <header>} nativa: los iconos sociales viven dentro de un contenedor
 * {@code .h-social-icons} que puede aparecer tanto en la cabecera como en el
 * pie. Además, el canal de Telegram aparece en el footer como un enlace de
 * botón con texto "Canal Telegram" (no como icono social).</p>
 *
 * <p>Por eso se escanea el documento entero en dos pasadas:
 * <ol>
 *   <li>Primera pasada: todos los anchors dentro de {@code .h-social-icons}
 *       (garantiza cubrir los iconos del Colibri).</li>
 *   <li>Segunda pasada: cualquier anchor del documento cuya URL coincida con
 *       un dominio de red social conocido (Facebook, Instagram, YouTube,
 *       WhatsApp, Telegram, X/Twitter, TikTok, LinkedIn…).</li>
 * </ol>
 * Deduplicamos por URL exacta: {@code whatsapp.com/channel/...} y
 * {@code wa.me/...} son entradas distintas aunque ambas pertenezcan a
 * WhatsApp.</p>
 */
@Component // Bean Spring registrado por la factory de estrategias.
public class SocialsScrapingStrategy implements ScrapingStrategy<List<SocialLinkDTO>> {

    // Logger operativo.
    private static final Logger log = LoggerFactory.getLogger(SocialsScrapingStrategy.class);

    // Mapa dominio → nombre canónico de red. Orden de inserción = prioridad de detección.
    private static final Map<String, String> DOMAIN_TO_NETWORK = buildDomainMap();

    @Override
    public ScrapingType getType() {
        return ScrapingType.SOCIALS; // Identificador consumido por la factory.
    }

    @Override
    public List<SocialLinkDTO> extract(Document document) {
        // LinkedHashMap preserva orden de inserción y deduplica por URL.
        Map<String, SocialLinkDTO> byUrl = new LinkedHashMap<>();

        // 1. Colibri social icons (contenedor específico del tema).
        Elements colibriIcons = document.select(".h-social-icons a[href]");
        collectAnchors(colibriIcons, byUrl);
        log.debug("Iconos Colibri encontrados: {}", colibriIcons.size());

        // 2. Cualquier otro anchor del documento que apunte a un dominio conocido.
        Elements allAnchors = document.select("a[href]");
        collectAnchors(allAnchors, byUrl);

        List<SocialLinkDTO> result = new ArrayList<>(byUrl.values()); // Lista ordenada por primer hallazgo.
        log.info("Redes sociales detectadas: {}", result.size());
        return result;
    }

    /**
     * Itera una lista de anchors y registra cada uno cuyo href coincida con
     * un dominio de red social conocido. Deduplica por URL absoluta.
     */
    private void collectAnchors(Elements anchors, Map<String, SocialLinkDTO> byUrl) {
        for (Element a : anchors) {
            String href = a.attr("abs:href");                                   // URL absoluta resuelta por Jsoup.
            if (href == null || href.isBlank()) continue;                       // Ignoramos anchors sin destino útil.

            String network = classifyNetwork(href);                             // Clasificación por dominio.
            if (network == null) continue;                                      // Dominio no reconocido → saltamos.

            byUrl.putIfAbsent(href, new SocialLinkDTO(network, href));          // Primer hallazgo para esa URL gana.
        }
    }

    /**
     * Clasifica la URL en una red social conocida.
     * @return nombre canónico de la red o {@code null} si no hay match.
     */
    private String classifyNetwork(String href) {
        String lower = href.toLowerCase(Locale.ROOT);                           // Comparación case-insensitive.
        for (Map.Entry<String, String> entry : DOMAIN_TO_NETWORK.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();        // Primer dominio match determina la red.
        }
        return null;                                                            // Sin coincidencia = no es red social.
    }

    /**
     * Construye la tabla de dominios reconocidos. Mantener orden para priorizar
     * dominios más específicos (p. ej. {@code youtu.be} antes que {@code youtube.com}).
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
