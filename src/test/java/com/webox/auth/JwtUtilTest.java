package com.webox.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret");
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 1L);
    }

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtUtil.generateToken(42L, "a@b.com");
        assertEquals(42L, jwtUtil.parseUserId(token));
    }

    @Test
    void parseInvalidTokenReturnsNull() {
        assertNull(jwtUtil.parseUserId("not-a-jwt"));
    }

    @Test
    void parseTokenSignedWithOtherSecretReturnsNull() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "another-secret");
        ReflectionTestUtils.setField(other, "expireHours", 1L);
        String token = other.generateToken(1L, "a@b.com");
        assertNull(jwtUtil.parseUserId(token));
    }

    @Test
    void parseExpiredTokenReturnsNull() {
        ReflectionTestUtils.setField(jwtUtil, "expireHours", -1L);
        String token = jwtUtil.generateToken(1L, "a@b.com");
        assertNull(jwtUtil.parseUserId(token));
    }
}
