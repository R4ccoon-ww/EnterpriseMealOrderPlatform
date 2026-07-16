package com.webox.service;

import com.webox.auth.JwtUtil;
import com.webox.common.BizException;
import com.webox.dto.LoginRequest;
import com.webox.dto.LoginResponse;
import com.webox.dto.RegisterRequest;
import com.webox.entity.User;
import com.webox.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, encoder, jwtUtil);
    }

    private RegisterRequest registerReq() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@corp.com");
        req.setPassword("pass123");
        req.setName("Alice");
        return req;
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userMapper.findByEmail("alice@corp.com")).thenReturn(new User());

        BizException e = assertThrows(BizException.class, () -> authService.register(registerReq()));
        assertEquals(4001, e.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void registerEncryptsPassword() {
        when(userMapper.findByEmail("alice@corp.com")).thenReturn(null);
        when(userMapper.findById(any())).thenReturn(new User());

        authService.register(registerReq());

        verify(userMapper).insert(org.mockito.ArgumentMatchers.argThat(u ->
                !"pass123".equals(u.getPassword()) && encoder.matches("pass123", u.getPassword())));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userMapper.findByEmail("nobody@corp.com")).thenReturn(null);
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@corp.com");
        req.setPassword("pass123");

        BizException e = assertThrows(BizException.class, () -> authService.login(req));
        assertEquals(4002, e.getCode());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setPassword(encoder.encode("correct"));
        when(userMapper.findByEmail("alice@corp.com")).thenReturn(user);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@corp.com");
        req.setPassword("wrong");

        BizException e = assertThrows(BizException.class, () -> authService.login(req));
        assertEquals(4003, e.getCode());
    }

    @Test
    void loginReturnsTokenOnSuccess() {
        User user = new User();
        user.setId(7L);
        user.setEmail("alice@corp.com");
        user.setPassword(encoder.encode("pass123"));
        when(userMapper.findByEmail("alice@corp.com")).thenReturn(user);
        when(jwtUtil.generateToken(anyLong(), any())).thenReturn("jwt-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@corp.com");
        req.setPassword("pass123");

        LoginResponse resp = authService.login(req);
        assertEquals("jwt-token", resp.getToken());
        assertEquals(7L, resp.getUser().getId());
    }
}
