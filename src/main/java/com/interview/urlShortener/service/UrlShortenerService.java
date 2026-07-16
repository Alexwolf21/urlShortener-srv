package com.interview.urlShortener.service;

import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.dto.ShortenResponse;
import com.interview.urlShortener.entity.UrlMapping;
import com.interview.urlShortener.exception.AliasConflictException;
import com.interview.urlShortener.exception.InvalidUrlException;
import com.interview.urlShortener.exception.UrlNotFoundException;
import com.interview.urlShortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final String baseUrl;

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");

    public UrlShortenerService(
            UrlMappingRepository urlMappingRepository,
            @Value("${app.base-url}") String baseUrl) {
        this.urlMappingRepository = urlMappingRepository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {
        String longUrl = request.longUrl();
        validateUrl(longUrl);

        String shortCode;
        if (request.customAlias() != null && !request.customAlias().trim().isEmpty()) {
            String alias = request.customAlias().trim();
            validateCustomAlias(alias);
            
            // Check if already taken
            if (urlMappingRepository.findByShortCode(alias).isPresent()) {
                throw new AliasConflictException("The custom alias '" + alias + "' is already in use.");
            }
            shortCode = alias;
        } else {
            // PR-2 temporary generator: random truncated UUID
            // We will transition to the collision-free counter sequence generator in PR-3
            shortCode = generateTempShortCode();
        }

        UrlMapping mapping = new UrlMapping(
            shortCode,
            longUrl,
            LocalDateTime.now(),
            null // Expiry can be set optionally, defaulting to null
        );

        UrlMapping saved = urlMappingRepository.save(mapping);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL code '" + shortCode + "' not found."));

        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException("Short URL code '" + shortCode + "' has expired.");
        }

        return mapping.getLongUrl();
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidUrlException("URL cannot be empty.");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL scheme must be http or https.");
            }

            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new InvalidUrlException("URL host is missing or invalid.");
            }

            // Prevent self-referencing to avoid infinite redirection loops
            URI baseUri = new URI(this.baseUrl);
            if (uri.getHost().equalsIgnoreCase(baseUri.getHost())) {
                throw new InvalidUrlException("Self-referential URL shortening is not allowed.");
            }
        } catch (Exception e) {
            throw new InvalidUrlException("Invalid URL format: " + e.getMessage());
        }
    }

    private void validateCustomAlias(String alias) {
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new InvalidUrlException("Custom alias must be alphanumeric, between 3 and 30 characters, and may only contain hyphens or underscores.");
        }
    }

    private String generateTempShortCode() {
        // Truncate UUID to 8 characters. We check uniqueness just in case for this temporary PR-2 solution.
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (urlMappingRepository.findByShortCode(code).isPresent());
        return code;
    }

    private ShortenResponse mapToResponse(UrlMapping mapping) {
        return new ShortenResponse(
            mapping.getShortCode(),
            this.baseUrl + mapping.getShortCode(),
            mapping.getLongUrl(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt()
        );
    }
}
