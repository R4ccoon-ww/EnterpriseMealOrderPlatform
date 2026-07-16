package com.webox.auth;

import com.webox.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 解析 Authorization: Bearer &lt;token&gt;，将 userId 写入 request attribute。
 * 拦截路径在 WebMvcConfig 中配置。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = resolveUserId(request);
        request.setAttribute(ATTR_USER_ID, userId);
        return true;
    }

    /**
     * 从 Authorization 头解析 userId；无 token 或 token 无效时抛 BizException(401)。
     */
    public Long resolveUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return jwtUtil.parseUserId(auth.substring(7));
    }
}
