package com.interview.urlShortener.service;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!redis")
public class DatabaseCounterService implements CounterService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCounterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long getNextId() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR url_sequence", Long.class);
        if (nextVal == null) {
            throw new IllegalStateException("Failed to retrieve next sequence value from database.");
        }
        return nextVal;
    }
}
