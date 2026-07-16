package com.webox.auth;

import com.webox.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 解析 Authorization: Bearer <token>，将 userId 写入 request attribute。
 * 拦截路径在 WebMvcConfig 中配置；菜单接口不拦截但可选登录，
 * 因此这里对放行路径也尽量解析 token（见 WebMvcConfig 注释）。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = resolveUserId(request);
        if (userId == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        request.setAttribute(ATTR_USER_ID, userId);
        return true;
    }

    public Long resolveUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return jwtUtil.parseUserId(auth.substring(7));
    }
}
