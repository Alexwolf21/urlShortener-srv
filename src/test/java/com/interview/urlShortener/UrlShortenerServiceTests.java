package com.interview.urlShortener;

import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.dto.ShortenResponse;
import com.interview.urlShortener.entity.UrlMapping;
import com.interview.urlShortener.exception.AliasConflictException;
import com.interview.urlShortener.exception.InvalidUrlException;
import com.interview.urlShortener.exception.UrlNotFoundException;
import com.interview.urlShortener.repository.UrlMappingRepository;
import com.interview.urlShortener.service.CounterService;
import com.interview.urlShortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTests {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private CounterService counterService;

    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        urlShortenerService = new UrlShortenerService(urlMappingRepository, counterService, "http://localhost:8080");
    }

    @Test
    void shortenUrl_withValidUrl_shouldSucceed() {
        // Arrange
        ShortenRequest request = new ShortenRequest("https://www.google.com", null);
        when(counterService.getNextId()).thenReturn(100000L);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortenResponse response = urlShortenerService.shortenUrl(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.longUrl()).isEqualTo("https://www.google.com");
        assertThat(response.shortCode()).isNotEmpty();
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + response.shortCode());
        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    void shortenUrl_withInvalidUrl_shouldThrowInvalidUrlException() {
        // Arrange
        ShortenRequest requestEmpty = new ShortenRequest("", null);
        ShortenRequest requestNoScheme = new ShortenRequest("google.com", null);
        ShortenRequest requestFtp = new ShortenRequest("ftp://google.com", null);

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.shortenUrl(requestEmpty))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("URL cannot be empty");

        assertThatThrownBy(() -> urlShortenerService.shortenUrl(requestNoScheme))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("URL scheme must be http or https");

        assertThatThrownBy(() -> urlShortenerService.shortenUrl(requestFtp))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("URL scheme must be http or https");
    }

    @Test
    void shortenUrl_withSelfReferencingUrl_shouldThrowInvalidUrlException() {
        // Arrange
        ShortenRequest requestSelf = new ShortenRequest("http://localhost:8080/somepath", null);

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.shortenUrl(requestSelf))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Self-referential URL shortening is not allowed");
    }

    @Test
    void shortenUrl_withValidCustomAlias_shouldSucceed() {
        // Arrange
        ShortenRequest request = new ShortenRequest("https://www.google.com", "google-search");
        when(urlMappingRepository.findByShortCode("google-search")).thenReturn(Optional.empty());
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortenResponse response = urlShortenerService.shortenUrl(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo("google-search");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/google-search");
        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    void shortenUrl_withDuplicateCustomAlias_shouldThrowAliasConflictException() {
        // Arrange
        ShortenRequest request = new ShortenRequest("https://www.google.com", "google-search");
        UrlMapping existing = new UrlMapping("google-search", "https://yahoo.com", LocalDateTime.now(), null);
        when(urlMappingRepository.findByShortCode("google-search")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.shortenUrl(request))
                .isInstanceOf(AliasConflictException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void shortenUrl_withInvalidCustomAlias_shouldThrowInvalidUrlException() {
        // Arrange
        ShortenRequest shortAlias = new ShortenRequest("https://www.google.com", "go");
        ShortenRequest specialCharAlias = new ShortenRequest("https://www.google.com", "go@gle");

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.shortenUrl(shortAlias))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Custom alias must be alphanumeric");

        assertThatThrownBy(() -> urlShortenerService.shortenUrl(specialCharAlias))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Custom alias must be alphanumeric");
    }

    @Test
    void getOriginalUrl_withExistingCode_shouldReturnUrl() {
        // Arrange
        UrlMapping mapping = new UrlMapping("abc", "https://www.google.com", LocalDateTime.now(), null);
        when(urlMappingRepository.findByShortCode("abc")).thenReturn(Optional.of(mapping));

        // Act
        String result = urlShortenerService.getOriginalUrl("abc");

        // Assert
        assertThat(result).isEqualTo("https://www.google.com");
    }

    @Test
    void getOriginalUrl_withExpiredCode_shouldThrowUrlNotFoundException() {
        // Arrange
        UrlMapping mapping = new UrlMapping("abc", "https://www.google.com", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        when(urlMappingRepository.findByShortCode("abc")).thenReturn(Optional.of(mapping));

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.getOriginalUrl("abc"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getOriginalUrl_withNonExistingCode_shouldThrowUrlNotFoundException() {
        // Arrange
        when(urlMappingRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> urlShortenerService.getOriginalUrl("missing"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
