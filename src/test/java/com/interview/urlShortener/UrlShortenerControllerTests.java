package com.interview.urlShortener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
    }

    @Test
    void testShortenAndRedirectFlow() throws Exception {
        ShortenRequest request = new ShortenRequest("https://www.google.com", null);

        // 1. Shorten the URL
        String responseContent = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andExpect(jsonPath("$.longUrl", is("https://www.google.com")))
                .andReturn().getResponse().getContentAsString();

        String shortCode = objectMapper.readTree(responseContent).get("shortCode").asText();

        // 2. Redirect back
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    void testShortenWithCustomAlias() throws Exception {
        ShortenRequest request = new ShortenRequest("https://www.yahoo.com", "my-yahoo");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is("my-yahoo")));

        // Try to shorten again with same alias, should return 409 Conflict
        ShortenRequest duplicateRequest = new ShortenRequest("https://www.bing.com", "my-yahoo");
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    @Test
    void testRedirectNotFound() throws Exception {
        mockMvc.perform(get("/non-existent-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testShortenInvalidUrl() throws Exception {
        ShortenRequest request = new ShortenRequest("invalid-url", null);

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid URL")));
    }
}
