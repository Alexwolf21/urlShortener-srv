package com.interview.urlShortener.service.analytics;

import com.interview.urlShortener.entity.AnalyticsLog;
import com.interview.urlShortener.repository.AnalyticsLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LinkClickListener {

    private final AnalyticsLogRepository analyticsLogRepository;

    public LinkClickListener(AnalyticsLogRepository analyticsLogRepository) {
        this.analyticsLogRepository = analyticsLogRepository;
    }

    @Async
    @EventListener
    public void handleLinkClickEvent(LinkClickEvent event) {
        AnalyticsLog log = new AnalyticsLog(
            event.shortCode(),
            event.clickedAt(),
            event.ipAddress(),
            event.userAgent(),
            event.referrer()
        );
        analyticsLogRepository.save(log);
    }
}
