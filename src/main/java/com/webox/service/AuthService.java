package com.webox.service;

import com.webox.auth.JwtUtil;
import com.webox.common.BizException;
import com.webox.dto.LoginRequest;
import com.webox.dto.LoginResponse;
import com.webox.dto.RegisterRequest;
import com.webox.entity.User;
import com.webox.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public User register(RegisterRequest req) {
        if (userMapper.findByEmail(req.getEmail()) != null) {
            throw new BizException(4001, "该邮箱已注册");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.findByEmail(req.getEmail());
        if (user == null) {
            throw new BizException(4002, "该邮箱尚未注册");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(4003, "密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, user);
    }
}
