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

    @GetMapping("/home")
    public Map<String, Object> getContent() throws IOException {
        return webScrapingService.scrapeWebsite();
    }
}

