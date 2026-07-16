package com.webox.auth;

import com.webox.common.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-at-least-16-chars");
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 1L);
    }

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtUtil.generateToken(42L, "a@b.com");
        assertEquals(42L, jwtUtil.parseUserId(token));
    }

    @Test
    void parseInvalidTokenThrows() {
        BizException e = assertThrows(BizException.class, () -> jwtUtil.parseUserId("not-a-jwt"));
        assertEquals(401, e.getCode());
    }

    @Test
    void parseTokenFromOtherSecretThrows() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "another-test-secret-1234");
        ReflectionTestUtils.setField(other, "expireHours", 1L);
        String token = other.generateToken(1L, "a@b.com");

        BizException e = assertThrows(BizException.class, () -> jwtUtil.parseUserId(token));
        assertEquals(401, e.getCode());
        assertEquals("Token 无效", e.getMessage());
    }

    @Test
    void parseExpiredTokenThrows() {
        ReflectionTestUtils.setField(jwtUtil, "expireHours", -1L);
        String token = jwtUtil.generateToken(1L, "a@b.com");

        BizException e = assertThrows(BizException.class, () -> jwtUtil.parseUserId(token));
        assertEquals(401, e.getCode());
        assertEquals("登录已过期，请重新登录", e.getMessage());
    }
}
