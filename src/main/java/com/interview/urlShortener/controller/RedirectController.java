package com.interview.urlShortener.controller;

import com.interview.urlShortener.service.UrlShortenerService;
import com.interview.urlShortener.service.analytics.LinkClickEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ApplicationEventPublisher eventPublisher;

    public RedirectController(UrlShortenerService urlShortenerService, ApplicationEventPublisher eventPublisher) {
        this.urlShortenerService = urlShortenerService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirectToOriginal(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = urlShortenerService.getOriginalUrl(code);

        // Capture client request details
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        // Publish event asynchronously for non-blocking persistence
        eventPublisher.publishEvent(new LinkClickEvent(
                code,
                LocalDateTime.now(),
                ipAddress,
                userAgent,
                referrer
        ));

        return ResponseEntity.status(HttpStatus.FOUND) // 302 Found temporary redirection
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
