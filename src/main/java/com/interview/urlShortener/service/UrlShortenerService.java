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
import java.util.regex.Pattern;
import com.interview.urlShortener.service.encoder.Base62Encoder;
import com.interview.urlShortener.service.encoder.FeistelObfuscator;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final SequenceService sequenceService;
    private final String baseUrl;

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");

    public UrlShortenerService(
            UrlMappingRepository urlMappingRepository,
            SequenceService sequenceService,
            @Value("${app.base-url}") String baseUrl) {
        this.urlMappingRepository = urlMappingRepository;
        this.sequenceService = sequenceService;
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
            // Collision-free counter sequence generator using Feistel Obfuscation
            shortCode = generateCollisionFreeShortCode();
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

    private String generateCollisionFreeShortCode() {
        long seqId = sequenceService.getNextId();
        long obfuscatedId = FeistelObfuscator.obfuscate(seqId);
        return Base62Encoder.encode(obfuscatedId);
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
