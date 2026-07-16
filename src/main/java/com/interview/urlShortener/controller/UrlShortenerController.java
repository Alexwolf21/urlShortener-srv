package com.interview.urlShortener.controller;

import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.dto.ShortenResponse;
import com.interview.urlShortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/shorten", "/shorten"})
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping
    public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request) {
        ShortenResponse response = urlShortenerService.shortenUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
