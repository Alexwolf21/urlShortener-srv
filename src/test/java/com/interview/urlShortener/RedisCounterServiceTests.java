package com.interview.urlShortener;

import com.interview.urlShortener.service.RedisCounterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCounterServiceTests {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCounterService redisCounterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisCounterService = new RedisCounterService(redisTemplate);
    }

    @Test
    void testNextId_allocatesSegmentAndIncrementsLocally() {
        // Arrange: Redis returns 1000 when incremented (first range: 1 - 1000)
        when(valueOperations.increment("global_url_counter", 1000L)).thenReturn(1000L);

        // Act & Assert
        // First call should trigger Redis network call and return 1
        long id1 = redisCounterService.getNextId();
        assertThat(id1).isEqualTo(1L);

        // Second call should increment locally without hitting Redis again
        long id2 = redisCounterService.getNextId();
        assertThat(id2).isEqualTo(2L);

        // Verify Redis was only incremented once
        verify(valueOperations, times(1)).increment("global_url_counter", 1000L);
    }

    @Test
    void testNextId_allocatesNewSegmentWhenExhausted() {
        // Arrange
        // First range: 1 - 1000
        when(valueOperations.increment("global_url_counter", 1000L)).thenReturn(1000L)
                // Second range: 1001 - 2000
                .thenReturn(2000L);

        // Act
        // Exhaust the first range of 1000
        for (int i = 0; i < 1000; i++) {
            redisCounterService.getNextId();
        }

        // The 1001st call should trigger another Redis call and return 1001
        long id1001 = redisCounterService.getNextId();
        assertThat(id1001).isEqualTo(1001L);

        // Verify Redis was incremented twice
        verify(valueOperations, times(2)).increment("global_url_counter", 1000L);
    }
}
