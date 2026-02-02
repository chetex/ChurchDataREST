package com.chetex.church.rest;

import com.chetex.church.rest.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RestApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(RestApplicationTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebScrapingService webScrapingServiceMock;

    @Test
    public void getHomePageImagesAndTextsTest() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("images", Arrays.asList("http://example.com/image1.jpg", "http://example.com/image2.jpg"));
        mockResponse.put("texts", Arrays.asList("Welcome to our church", "Join us for worship"));

        Mockito.when(webScrapingServiceMock.scrapHomePage()).thenReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/home")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.texts", hasSize(2)));

        log.info("getHomePageImagesAndTextsTest passed.");
    }

}