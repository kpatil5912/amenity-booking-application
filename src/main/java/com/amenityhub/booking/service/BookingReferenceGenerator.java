package com.amenityhub.booking.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class BookingReferenceGenerator {

    // Excludes ambiguous characters (0/O, 1/I).
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 8;
    private static final String PREFIX = "AMN-";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
