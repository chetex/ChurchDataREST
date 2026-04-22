package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.NavigationItemDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NavigationScrapingStrategy implements ScrapingStrategy<List<NavigationItemDTO>> {

    private static final Logger log = LoggerFactory.getLogger(NavigationScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.NAVIGATION;
    }

    @Override
    public List<NavigationItemDTO> extract(Document document) {
        List<NavigationItemDTO> navigationItems = new ArrayList<>();

        // Robust and universal navigation selector:
        // Try various common navigation wrappers, from most specific to more generic
        Elements topLevelItems = document.select("nav.elementor-nav-menu--main > ul > li");

        if (topLevelItems.isEmpty()) {
            log.debug("No items found with 'nav.elementor-nav-menu--main > ul > li'. Trying fallback.");
            topLevelItems = document.select("nav ul li, header ul li, .main-navigation ul li, #main-menu > li, .menu > li");
        }

        // Additional fallback: Get the first large-looking <ul> within a <nav> or <header>
        if (topLevelItems.isEmpty()) {
            log.debug("No items found with generic li selectors. Trying broader fallback.");
            Element mainNav = document.selectFirst("nav, header, [id*=menu], [class*=menu]");
            if (mainNav != null) {
                topLevelItems = mainNav.select("ul > li");
            }
        }

        for (Element li : topLevelItems) {
            Element anchor = li.selectFirst("a");
            if (anchor == null) continue;

            String name = !anchor.ownText().isEmpty() ? anchor.ownText() : anchor.text();
            String url = anchor.attr("abs:href");

            // Ignore anchor links, empty names, or invalid URLs
            if (name.isEmpty() || url.isEmpty() || url.equals("#") || name.length() > 50) continue;

            // Avoid duplicating items if multiple selectors hit the same elements
            if (navigationItems.stream().anyMatch(item -> item.getUrl().equals(url))) continue;

            NavigationItemDTO mainItem = new NavigationItemDTO(name.trim(), url);

            // Sub-menu extraction (nested ul)
            Elements subItems = li.select("ul li a");
            for (Element subAnchor : subItems) {
                String subName = !subAnchor.ownText().isEmpty() ? subAnchor.ownText() : subAnchor.text();
                String subUrl = subAnchor.attr("abs:href");
                if (!subName.isEmpty() && !subUrl.isEmpty() && !subUrl.equals("#") && !subUrl.equals(url)) {
                    mainItem.addSubItem(new NavigationItemDTO(subName.trim(), subUrl));
                }
            }
            navigationItems.add(mainItem);
        }

        // Dedupe pass: items that appear as subItems of another top-level entry must not be
        // duplicated at the top level (e.g. "Contactar", "Horarios" appear both under "Contacto"
        // and as flat items when the site renders the mobile drawer alongside the desktop menu).
        Set<String> subItemUrls = new HashSet<>();
        for (NavigationItemDTO item : navigationItems) {
            if (item.getSubItems() == null) continue;
            for (NavigationItemDTO sub : item.getSubItems()) {
                if (sub.getUrl() != null && !sub.getUrl().isBlank()) {
                    subItemUrls.add(sub.getUrl());
                }
            }
        }
        List<NavigationItemDTO> deduped = new ArrayList<>(navigationItems.size());
        for (NavigationItemDTO item : navigationItems) {
            // Keep top-level items that have children OR whose URL does not appear in any sibling's subItems.
            boolean hasChildren = item.getSubItems() != null && !item.getSubItems().isEmpty();
            if (hasChildren || !subItemUrls.contains(item.getUrl())) {
                deduped.add(item);
            }
        }

        log.info("Extracted {} navigation items ({} dropped as duplicates of subItems).",
                deduped.size(), navigationItems.size() - deduped.size());
        return deduped;
    }
}
