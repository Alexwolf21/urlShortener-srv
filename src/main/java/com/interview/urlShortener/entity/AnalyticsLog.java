package com.interview.urlShortener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_logs", indexes = {
    @Index(name = "idx_analytics_short_code", columnList = "shortCode")
})
public class AnalyticsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Column(length = 1000)
    private String referrer;

    public AnalyticsLog() {
    }

    public AnalyticsLog(String shortCode, LocalDateTime clickedAt, String ipAddress, String userAgent, String referrer) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}
