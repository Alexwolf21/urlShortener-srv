package com.interview.urlShortener.controller;

import com.interview.urlShortener.service.UrlShortenerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UrlShortenerService urlShortenerService;

    public AdminController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupInactiveUrls(@RequestParam(defaultValue = "30") int days) {
        int deletedCount = urlShortenerService.cleanupInactiveUrls(days);
        return ResponseEntity.ok(Map.of(
                "message", "Database retention cleanup executed successfully.",
                "deletedCount", deletedCount,
                "retentionDays", days
        ));
    }
}
