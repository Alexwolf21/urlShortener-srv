package com.interview.urlShortener;

import com.interview.urlShortener.entity.UrlMapping;
import com.interview.urlShortener.repository.UrlMappingRepository;
import com.interview.urlShortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class CachingTests {

    @Autowired
    private UrlShortenerService urlShortenerService;

    @MockBean
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testGetOriginalUrl_isCached() {
        String shortCode = "cachedCode";
        UrlMapping mapping = new UrlMapping(shortCode, "https://www.google.com", LocalDateTime.now(), null);
        
        // Evict the key if already in cache
        if (cacheManager.getCache("urls") != null) {
            cacheManager.getCache("urls").evict(shortCode);
        }

        // Mock repository to return the mapping
        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        // First call - should hit repository
        String url1 = urlShortenerService.getOriginalUrl(shortCode);
        assertThat(url1).isEqualTo("https://www.google.com");
        
        // Second call - should hit cache and NOT hit repository again
        String url2 = urlShortenerService.getOriginalUrl(shortCode);
        assertThat(url2).isEqualTo("https://www.google.com");

        // Verify repository findByShortCode was only called once
        verify(urlMappingRepository, times(1)).findByShortCode(shortCode);
        
        // Verify cache contains the value
        assertThat(cacheManager.getCache("urls")).isNotNull();
        assertThat(cacheManager.getCache("urls").get(shortCode)).isNotNull();
    }
}
