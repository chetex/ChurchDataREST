package com.chetex.church.rest;

import com.chetex.church.rest.service.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.boot.test.web.client.*;
import org.springframework.boot.test.web.server.*;
import org.springframework.core.*;
import org.springframework.http.*;
import org.springframework.test.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.web.servlet.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RestApplicationTests {

	@LocalServerPort
	private int port;

	@Mock
	private WebScrapingService webScrapingServiceMock;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TestRestTemplate restTemplate;

	/**
	 * This method test the getGradebookController call HTTP GET method
	 * MockMvc simulates the request and returns a MvcResult object
	 * @throws Exception Exception
	 */
	@Test
	public void getHomePageImagesAndTextsTest() throws Exception {
		// call perform mock mvc
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/home"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json")) // Is json content type
				.andExpect(jsonPath("$.images", hasSize(greaterThan(0)))) // Verify that the images list is not empty
				.andReturn();

		// Convert the MvcResult to a Map
		Map<String, Object> resultMap = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);

		// Verify that the result map contains the expected keys
		assertThat(resultMap.keySet(), hasItems("images", "texts"));

	}
}
