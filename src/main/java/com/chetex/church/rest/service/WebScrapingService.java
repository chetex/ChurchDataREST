package com.chetex.church.rest.service;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

@Service
public class WebScrapingService {

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
        Elements images = document.select("img");
        List<String> imageUrls = new ArrayList<>();
        for (Element img : images) {
            String src = img.attr("abs:src");
            if (!src.isEmpty()) {
                imageUrls.add(src);
            }
        }
        return imageUrls;
    }
}

