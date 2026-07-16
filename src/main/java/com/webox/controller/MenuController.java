package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.entity.MenuItem;
import com.webox.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 菜单接口公开访问；带 token 且 recommend=true 时按用户偏好过滤/排序（加分项 A）。
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final JwtInterceptor jwtInterceptor;

    @GetMapping
    public ApiResponse<List<MenuItem>> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false, defaultValue = "false") boolean recommend,
                                            HttpServletRequest request) {
        Long userId = jwtInterceptor.resolveUserId(request);
        return ApiResponse.ok(menuService.list(category, recommend ? userId : null));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuItem> detail(@PathVariable String id) {
        return ApiResponse.ok(menuService.getById(id));
    }
}
