package com.interview.urlShortener.dto;

public record ShortenRequest(
    String longUrl,
    String customAlias
) {}
