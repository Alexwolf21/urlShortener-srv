package com.interview.urlShortener.service.analytics;

import java.time.LocalDateTime;

public record LinkClickEvent(
    String shortCode,
    LocalDateTime clickedAt,
    String ipAddress,
    String userAgent,
    String referrer
) {}
