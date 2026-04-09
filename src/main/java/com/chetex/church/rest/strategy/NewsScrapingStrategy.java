package com.chetex.church.rest.strategy;

import com.chetex.church.rest.dto.NewsItemDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NewsScrapingStrategy implements ScrapingStrategy<List<NewsItemDTO>> {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapingStrategy.class);

    @Override
    public ScrapingType getType() {
        return ScrapingType.NEWS;
    }

    @Override
    public List<NewsItemDTO> extract(Document document) {
        List<NewsItemDTO> newsItems = new ArrayList<>();

        // On https://www.parroquiasanpablovi.es/actualidad/ (and home if present)
        // Each news item is within an article tag or a div with elementor-post class
        Elements articles = document.select("article.elementor-post, div.elementor-post");

        if (articles.isEmpty()) {
            log.warn("No news items found with primary selectors. Trying fallback selectors.");
            // Fallback for different Elementor layouts or generic WordPress posts
            articles = document.select("article, .post, .entry");
        }

        for (Element article : articles) {
            String title = "";
            String subtitle = "";
            String imageUrl = "";
            String link = "";
            String excerpt = "";

            // Title and link
            Element titleElement = article.selectFirst(".elementor-post__title a, h2 a, h3 a, .entry-title a");
            if (titleElement != null) {
                title = titleElement.text();
                link = titleElement.attr("abs:href");
            } else {
                titleElement = article.selectFirst(".elementor-post__title, h2, h3");
                if (titleElement != null) {
                    title = titleElement.text();
                }
            }

            // Image
            Element imageElement = article.selectFirst(".elementor-post__thumbnail img, img");
            if (imageElement != null) {
                // Try to get the highest resolution image (some WP themes use lazy loading or srcsets)
                imageUrl = imageElement.attr("abs:src");
                if (imageUrl.isEmpty()) {
                    imageUrl = imageElement.attr("abs:data-src");
                }
            }

            // Excerpt
            Element excerptElement = article.selectFirst(".elementor-post__excerpt p, .entry-content p, .post-content p");
            if (excerptElement != null) {
                excerpt = excerptElement.text();
            }

            // Subtitle / Date (Optional meta-data)
            Element metaElement = article.selectFirst(".elementor-post__meta-data, .post-date, .entry-meta");
            if (metaElement != null) {
                subtitle = metaElement.text();
            }

            // Validate that we have at least a title and link
            if (!title.isEmpty() && !link.isEmpty() && !link.equals("#")) {
                newsItems.add(new NewsItemDTO(title, subtitle, imageUrl, link, excerpt));
            }
        }

        log.info("Successfully extracted {} news items.", newsItems.size());
        return newsItems;
    }
}
