package com.interview.urlShortener.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SequenceService {

    private final JdbcTemplate jdbcTemplate;

    public SequenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long getNextId() {
        // Query the next sequence value (Standard SQL syntax supported by both H2 and PostgreSQL)
        Long nextVal = jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR url_sequence", Long.class);
        if (nextVal == null) {
            throw new IllegalStateException("Failed to retrieve next sequence value from database.");
        }
        return nextVal;
    }
}
