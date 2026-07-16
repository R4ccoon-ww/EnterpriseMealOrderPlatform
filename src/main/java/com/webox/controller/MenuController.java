package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.entity.MenuItem;
import com.webox.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<List<MenuItem>> list(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false, defaultValue = "true") boolean recommend) {
        return ApiResponse.ok(menuService.list(category, recommend ? userId : null));
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuItem> detail(@PathVariable String id) {
        return ApiResponse.ok(menuService.getById(id));
    }
}
