package com.interview.urlShortener.controller;

import com.interview.urlShortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirectToOriginal(@PathVariable String code) {
        String originalUrl = urlShortenerService.getOriginalUrl(code);
        return ResponseEntity.status(HttpStatus.FOUND) // 302 Found temporary redirection
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
