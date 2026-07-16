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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingTests {

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
    void testRateLimitingEnforcedForSingleIP() throws Exception {
        ShortenRequest request = new ShortenRequest("https://www.google.com", null);
        String clientIp = "192.168.2.10";

        // Send 10 successful shorten requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(status().isCreated());
        }

        // The 11th request from the same IP should fail with 429 Too Many Requests
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testRateLimitingIsIndependentPerIP() throws Exception {
        ShortenRequest request = new ShortenRequest("https://www.google.com", null);
        String blockedIp = "192.168.3.10";
        String safeIp = "192.168.3.20";

        // Exhaust tokens for blockedIp
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(req -> {
                                req.setRemoteAddr(blockedIp);
                                return req;
                            }))
                    .andExpect(status().isCreated());
        }

        // Verify blockedIp is now rate limited (429)
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(req -> {
                            req.setRemoteAddr(blockedIp);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests());

        // Verify a DIFFERENT IP can still successfully shorten URLs
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(req -> {
                            req.setRemoteAddr(safeIp);
                            return req;
                        }))
                .andExpect(status().isCreated());
    }
}
