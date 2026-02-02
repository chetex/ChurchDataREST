package com.chetex.church.rest.controller;

import com.chetex.church.rest.service.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class WebScrapingController {

    @Autowired
    private WebScrapingService webScrapingService;

    /**
     * Extract home page test
     * @return Map with images and texts
     * @throws IOException Input output exception
     */
    @GetMapping("/home")
    public Map<String, Object> getContent() throws IOException {
        return webScrapingService.scrapHomePage();
    }

    /**
     * Scrape from home page, scrape menu items
     * @return List of menu items as Map objects
     * @throws IOException Input output exception
     */
    @GetMapping("/menu")
    public List<Map<String, String>> getMenuItems() throws IOException {
        return webScrapingService.scrapMenuItems();
    }
}

