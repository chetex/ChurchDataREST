package com.chetex.church.rest.service;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

@Service
public class WebScrapingService {
    // Add log4java logger
    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);

    @Value("${church.url}")
    private String churchUrl;

    /**
     * Scrape the website and extract images and texts
     * @return Map with images and texts
     * @throws IOException Exception
     */
    public Map<String, Object> scrapHomePage() throws IOException {
        // Connect to the URL and get the HTML document
        Document document = Jsoup.connect(churchUrl).get();

        // Extract images
        List<String> imageUrls = extractImages(document);

        // Extract texts
        List<String> textContents = extractTexts(document);

        // Crear el resultado
        Map<String, Object> result = new HashMap<>();
        result.put("images", imageUrls);
        result.put("texts", textContents);
        return result;
    }

    /**
     * Extract texts from the HTML document
     * @param document HTML document
     * @return List of text contents
     */
    private static List<String> extractTexts(Document document) {
        Elements texts = document.select("p, h1, h2, h3, h4, h5, h6");
        List<String> textContents = new ArrayList<>();
        for (Element text : texts) {
            textContents.add(text.text());
        }
        return textContents;
    }

    /**
     * Extract images from the HTML document
     * @param document HTML document
     * @return List of image URLs
     */
    private static List<String> extractImages(Document document) {
        Elements imagesElements = document.select("img");
        List<String> imageUrlsList = new ArrayList<>();
        for (Element img : imagesElements) {
            String src = img.attr("abs:src");
            if (!src.isEmpty()) {
                imageUrlsList.add(src);
            }
        }
        return imageUrlsList;
    }

    /**
     * Scrapes the menu items from the website.
     * It iterates through the primary menu and checks for sub-menus.
     * @return A list of maps containing "nombre" and "link".
     */
    public List<Map<String, String>> scrapMenuItems() {
        List<Map<String, String>> menuList = new ArrayList<>();

        try {
            // Connect to the church website
            Document document = Jsoup.connect(churchUrl).get();

            // 1. Select only the top-level <li> elements from the primary menu
            // We use the child combinator (>) to avoid selecting sub-li directly in the first loop
            Elements topLevelItems = document.select("#menu-primary > li");

            for (Element li : topLevelItems) {
                // Check if this <li> contains a sub-menu (ul with class 'sub-menu')
                Element subMenu = li.selectFirst("ul.sub-menu");

                if (subMenu != null) {
                    log.info("Sub-menu detected for: {}", li.selectFirst("a").ownText());

                    // 2. If it has a sub-menu, loop through the sub-menu <li> elements
                    Elements subMenuLinks = subMenu.select("li a");
                    for (Element subLink : subMenuLinks) {
                        addMenuItemToList(menuList, subLink);
                    }
                } else {
                    // 3. If it does NOT have a sub-menu, extract the direct <a> link
                    Element directLink = li.selectFirst("a");
                    if (directLink != null) {
                        addMenuItemToList(menuList, directLink);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error during menu scraping: {}", e.getMessage());
        }

        return menuList;
    }

    /**
     * Helper method to extract text and URL from an element and add it to the menuList.
     * * @param menuList The menuList to add the map to.
     * @param anchor The <a> element to extract data from.
     */
    private void addMenuItemToList(List<Map<String, String>> menuList, Element anchor) {
        // ownText() gets only the text of the <a>, excluding children like <svg> or <span>
        String name = anchor.ownText();

        // Fallback if ownText is empty (sometimes happens with specific CMS structures)
        if (name.isEmpty()) {
            name = anchor.text();
        }

        String url = anchor.attr("abs:href");

        // We only add valid links (ignoring placeholders like "#")
        if (!name.isEmpty() && !url.isEmpty() && !url.equals("#")) {
            Map<String, String> item = new HashMap<>();
            item.put("name", name);
            item.put("link", url);
            menuList.add(item);
        }
    }
}

