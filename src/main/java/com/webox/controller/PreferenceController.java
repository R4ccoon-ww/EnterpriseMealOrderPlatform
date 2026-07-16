package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.dto.PreferenceRequest;
import com.webox.entity.UserPreference;
import com.webox.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public ApiResponse<UserPreference> get(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId) {
        return ApiResponse.ok(preferenceService.get(userId));
    }

    @PutMapping
    public ApiResponse<UserPreference> save(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                            @RequestBody PreferenceRequest req) {
        return ApiResponse.ok(preferenceService.save(userId, req));
    }
}
