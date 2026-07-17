package com.interview.urlShortener;

import com.interview.urlShortener.entity.AnalyticsLog;
import com.interview.urlShortener.entity.UrlMapping;
import com.interview.urlShortener.repository.AnalyticsLogRepository;
import com.interview.urlShortener.repository.UrlMappingRepository;
import com.interview.urlShortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private AnalyticsLogRepository analyticsLogRepository;

    @BeforeEach
    void setUp() {
        analyticsLogRepository.deleteAll();
        urlMappingRepository.deleteAll();
    }

    @Test
    void testCleanupDeletesInactiveUrls() throws Exception {
        // 1. Create a URL mapping that has been active (clicked recently: 10 days ago)
        UrlMapping activeMapping = new UrlMapping("activeCode", "https://google.com", LocalDateTime.now().minusDays(40), null);
        urlMappingRepository.save(activeMapping);
        analyticsLogRepository.save(new AnalyticsLog("activeCode", LocalDateTime.now().minusDays(10), "127.0.0.1", null, null));

        // 2. Create a URL mapping that is inactive (clicked 35 days ago)
        UrlMapping inactiveMapping = new UrlMapping("inactiveCode", "https://yahoo.com", LocalDateTime.now().minusDays(40), null);
        urlMappingRepository.save(inactiveMapping);
        analyticsLogRepository.save(new AnalyticsLog("inactiveCode", LocalDateTime.now().minusDays(35), "127.0.0.1", null, null));

        // 3. Create a URL mapping that is old and has NEVER been clicked (created 40 days ago)
        UrlMapping neverClickedOld = new UrlMapping("neverClickedOld", "https://bing.com", LocalDateTime.now().minusDays(40), null);
        urlMappingRepository.save(neverClickedOld);

        // 4. Create a URL mapping that is new and has never been clicked (created today)
        UrlMapping neverClickedNew = new UrlMapping("neverClickedNew", "https://duckduckgo.com", LocalDateTime.now(), null);
        urlMappingRepository.save(neverClickedNew);

        // Assert setup counts
        assertThat(urlMappingRepository.count()).isEqualTo(4);

        // 5. Trigger the administrative cleanup API for 30-day retention
        mockMvc.perform(delete("/api/v1/admin/cleanup")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2)); // Expecting inactiveCode and neverClickedOld to be deleted

        // 6. Assert remaining mappings in database
        assertThat(urlMappingRepository.findByShortCode("activeCode")).isPresent();
        assertThat(urlMappingRepository.findByShortCode("neverClickedNew")).isPresent();
        assertThat(urlMappingRepository.findByShortCode("inactiveCode")).isNotPresent();
        assertThat(urlMappingRepository.findByShortCode("neverClickedOld")).isNotPresent();
    }
}
