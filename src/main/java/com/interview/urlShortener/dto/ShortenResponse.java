package com.interview.urlShortener.dto;

import java.time.LocalDateTime;

public record ShortenResponse(
    String shortCode,
    String shortUrl,
    String longUrl,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
