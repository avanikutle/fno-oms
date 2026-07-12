package com.fnooms.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BrokerConfig utility methods — masking and token expiry.
 * Critical: masked display must never expose full credentials in logs/UI.
 */
class BrokerConfigTest {

    @Test
    void getMaskedToken_showsOnlyLast6Chars() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setAccessToken("eyJhbGciOiJIUzI1NiJ9.ABCDEF");
        String masked = cfg.getMaskedToken();
        assertTrue(masked.startsWith("****"), "Must start with ****");
        assertTrue(masked.endsWith("ABCDEF"), "Must end with last 6 chars");
        assertFalse(masked.contains("eyJhbGci"), "Must not expose full token");
    }

    @Test
    void getMaskedToken_shortToken_returnsFourStars() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setAccessToken("abc");
        assertEquals("****", cfg.getMaskedToken());
    }

    @Test
    void getMaskedToken_nullToken_returnsFourStars() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setAccessToken(null);
        assertEquals("****", cfg.getMaskedToken());
    }

    @Test
    void getMaskedApiKey_showsFirstFourCharsOnly() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setApiKey("ay3xYzkBMAK1234");
        String masked = cfg.getMaskedApiKey();
        assertTrue(masked.startsWith("ay3x"), "Must start with first 4 chars");
        assertTrue(masked.endsWith("****"), "Must mask remainder");
        assertFalse(masked.contains("MAK"), "Must not expose full key");
    }

    @Test
    void isTokenExpired_pastExpiry_returnsTrue() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setTokenExpiry(Instant.now().minus(1, ChronoUnit.HOURS));
        assertTrue(cfg.isTokenExpired());
    }

    @Test
    void isTokenExpired_futureExpiry_returnsFalse() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        assertFalse(cfg.isTokenExpired());
    }

    @Test
    void isTokenExpired_nullExpiry_returnsTrue() {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setTokenExpiry(null);
        assertTrue(cfg.isTokenExpired(), "Null expiry should be treated as expired");
    }

    @Test
    void isActive_defaultFalse() {
        BrokerConfig cfg = new BrokerConfig();
        assertFalse(cfg.isActive(), "New config should not be active by default");
    }
}
