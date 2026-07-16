package com.interview.urlShortener.service.encoder;

public class FeistelObfuscator {

    private static final int ROUNDS = 4;
    private static final long HALF_MASK = (1L << 31) - 1; // 31 bits

    public static long obfuscate(long val) {
        if (val < 0 || val >= (1L << 62)) {
            throw new IllegalArgumentException("Value to obfuscate must be in the range [0, 2^62 - 1]");
        }

        // Split 62-bit value into two 31-bit halves
        long left = (val >>> 31) & HALF_MASK;
        long right = val & HALF_MASK;

        for (int i = 0; i < ROUNDS; i++) {
            long nextLeft = right;
            long nextRight = left ^ roundFunction(right, i);
            left = nextLeft;
            right = nextRight;
        }

        // Combine back into a 62-bit long
        return (left << 31) | right;
    }

    public static long deobfuscate(long val) {
        if (val < 0 || val >= (1L << 62)) {
            throw new IllegalArgumentException("Value to deobfuscate must be in the range [0, 2^62 - 1]");
        }

        // Split 62-bit value into two 31-bit halves
        long left = (val >>> 31) & HALF_MASK;
        long right = val & HALF_MASK;

        // Perform Feistel rounds in reverse order
        for (int i = ROUNDS - 1; i >= 0; i--) {
            long prevRight = left;
            long prevLeft = right ^ roundFunction(left, i);
            left = prevLeft;
            right = prevRight;
        }

        return (left << 31) | right;
    }

    private static long roundFunction(long val, int round) {
        // A simple mixing function using Knuth's multiplicative hashing and round keys
        long hash = val ^ (round * 0x9e3779b9L);
        hash = (hash ^ (hash >>> 16)) * 0x85ebca6bL;
        hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35L;
        hash = hash ^ (hash >>> 16);
        return hash & HALF_MASK;
    }
}
