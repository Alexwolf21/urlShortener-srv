package com.interview.urlShortener;

import com.interview.urlShortener.dto.ShortenRequest;
import com.interview.urlShortener.dto.ShortenResponse;
import com.interview.urlShortener.repository.UrlMappingRepository;
import com.interview.urlShortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTests {

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
    }

    @Test
    void testConcurrentShorteningWithoutCollisions() throws InterruptedException {
        int numberOfThreads = 32;
        int tasksPerThread = 15;
        int totalRequests = numberOfThreads * tasksPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        Set<String> shortCodes = ConcurrentHashMap.newKeySet();
        Set<Throwable> exceptions = Collections.synchronizedSet(new java.util.HashSet<>());

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    ShortenRequest request = new ShortenRequest("https://www.google.com/search?q=" + index, null);
                    ShortenResponse response = urlShortenerService.shortenUrl(request);
                    shortCodes.add(response.shortCode());
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all tasks simultaneously
        finishLatch.await(); // Wait for completion

        executorService.shutdown();

        // Assert that no exceptions (like unique constraint violations or database locking) occurred
        assertThat(exceptions).isEmpty();

        // Assert that every generated short code is unique
        assertThat(shortCodes).hasSize(totalRequests);

        // Assert that the database contains the correct number of records
        assertThat(urlMappingRepository.count()).isEqualTo(totalRequests);
    }
}
