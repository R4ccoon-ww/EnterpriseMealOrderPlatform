package com.webox.auth;

import com.webox.common.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${webox.jwt.secret}")
    private String secret;

    @Value("${webox.jwt.expire-hours}")
    private long expireHours;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT secret 长度至少 16 字符");
        }
        if (expireHours <= 0) {
            throw new IllegalArgumentException("JWT expire-hours 必须大于 0");
        }
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 解析 token，返回 userId；无效/过期时抛出 BizException(401)。
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new BizException(401, "登录已过期，请重新登录");
        } catch (SignatureException | MalformedJwtException e) {
            throw new BizException(401, "Token 无效");
        } catch (Exception e) {
            throw new BizException(401, "Token 解析失败");
        }
    }
}
