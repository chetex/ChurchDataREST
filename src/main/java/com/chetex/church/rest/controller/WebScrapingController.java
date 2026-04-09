package com.chetex.church.rest.controller;

import com.chetex.church.rest.dto.DetailPageDTO;
import com.chetex.church.rest.dto.HomeContentItemDTO;
import com.chetex.church.rest.dto.NavigationItemDTO;
import com.chetex.church.rest.dto.NewsItemDTO;
import com.chetex.church.rest.service.WebScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WebScrapingController {

    @Autowired
    private WebScrapingService webScrapingService;

    /**
     * Scrape menu items and return them as a list of NavigationItemDTO.
     * @return List of NavigationItemDTO objects representing the website's navigation.
     * @throws IOException Input output exception
     */
    @GetMapping("/navigation")
    public List<NavigationItemDTO> getNavigation() throws IOException {
        return webScrapingService.getNavigation();
    }

    /**
     * Scrape home news items and return them as a list of NewsItemDTO.
     * @return List of NewsItemDTO objects representing the news from the home page.
     * @throws IOException Input output exception
     */
    @GetMapping("/news")
    public List<NewsItemDTO> getNews() throws IOException {
        return webScrapingService.getHomeNews();
    }

    /**
     * Scrapea la home principal y todas las sub-páginas accesibles a través del menú
     * (menús y submenús, recursivamente), devolviendo un único listado agregado
     * de elementos con title, subtitle, text, image y linkUrl.
     *
     * @return Lista agregada de elementos de contenido.
     * @throws IOException si falla el scraping de la home principal.
     */
    @GetMapping("/home")
    public List<HomeContentItemDTO> getHome() throws IOException {
        return webScrapingService.getHomeAggregate();
    }

    /**
     * Scrape a detail page from a given URL and return its content as a DetailPageDTO.
     * @param url The URL of the detail page to scrape.
     * @return DetailPageDTO object containing the extracted information.
     * @throws IOException Input output exception
     */
    @GetMapping("/detail")
    public DetailPageDTO getDetailPage(@RequestParam String url) throws IOException {
        return webScrapingService.getDetailPage(url);
    }
}
