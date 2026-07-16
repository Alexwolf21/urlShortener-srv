package com.interview.urlShortener;

import com.interview.urlShortener.service.encoder.Base62Encoder;
import com.interview.urlShortener.service.encoder.FeistelObfuscator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncoderTests {

    @Test
    void testBase62RoundTrip() {
        long[] valuesToTest = {0L, 1L, 62L, 63L, 100000L, 9999999999L, Long.MAX_VALUE};
        for (long val : valuesToTest) {
            String encoded = Base62Encoder.encode(val);
            long decoded = Base62Encoder.decode(encoded);
            assertThat(decoded).isEqualTo(val);
        }
    }

    @Test
    void testBase62InvalidInputs() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Base62Encoder.decode(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Base62Encoder.decode(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Base62Encoder.decode("abc@123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testFeistelObfuscatorRoundTrip() {
        Random random = new Random();
        // Test 1000 random inputs
        for (int i = 0; i < 1000; i++) {
            long original = Math.abs(random.nextLong()) % (1L << 62);
            long obfuscated = FeistelObfuscator.obfuscate(original);
            long deobfuscated = FeistelObfuscator.deobfuscate(obfuscated);
            assertThat(deobfuscated).isEqualTo(original);
        }
    }

    @Test
    void testFeistelObfuscatorUniqueness() {
        int count = 50000;
        Set<Long> obfuscatedSet = new HashSet<>();

        // Generate obfuscated values for numbers 0 to count
        for (long i = 0; i < count; i++) {
            long obfuscated = FeistelObfuscator.obfuscate(i);
            obfuscatedSet.add(obfuscated);
        }

        // Since it is bijective, there should be exactly 'count' unique obfuscated values
        assertThat(obfuscatedSet).hasSize(count);
    }

    @Test
    void testFeistelInvalidRange() {
        assertThatThrownBy(() -> FeistelObfuscator.obfuscate(-1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FeistelObfuscator.obfuscate(1L << 62))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
