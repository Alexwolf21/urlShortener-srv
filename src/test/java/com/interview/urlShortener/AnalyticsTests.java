package com.interview.urlShortener;

import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.dto.ShortenResponse;
import com.interview.urlShortener.repository.AnalyticsLogRepository;
import com.interview.urlShortener.repository.UrlMappingRepository;
import com.interview.urlShortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsTests {

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
    void testLinkClickLogsAnalyticsAsynchronously() throws Exception {
        // 1. Shorten a URL
        ShortenResponse shortenResponse = urlShortenerService.shortenUrl(new ShortenRequest("https://www.google.com", null));
        String code = shortenResponse.shortCode();

        // 2. Perform a redirect request with specific headers
        mockMvc.perform(get("/" + code)
                        .header("User-Agent", "TestBrowser")
                        .header("Referer", "http://testreferrer.com")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.50");
                            return request;
                        }))
                .andExpect(status().isFound());

        // 3. Since click logging is asynchronous, sleep briefly for database save
        Thread.sleep(500);

        // 4. Verify that the analytics log was saved correctly in database
        var logs = analyticsLogRepository.findByShortCode(code);
        assertThat(logs).hasSize(1);
        
        var log = logs.get(0);
        assertThat(log.getShortCode()).isEqualTo(code);
        assertThat(log.getIpAddress()).isEqualTo("192.168.1.50");
        assertThat(log.getUserAgent()).isEqualTo("TestBrowser");
        assertThat(log.getReferrer()).isEqualTo("http://testreferrer.com");
        assertThat(log.getClickedAt()).isNotNull();
    }
}
