package com.interview.urlShortener.repository;

import com.interview.urlShortener.entity.AnalyticsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsLogRepository extends JpaRepository<AnalyticsLog, Long> {
    List<AnalyticsLog> findByShortCode(String shortCode);
}
