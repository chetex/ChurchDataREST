package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.NavigationItemDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

        log.info("Extracted {} navigation items using universal selectors.", navigationItems.size());
        return navigationItems;
    }
}
