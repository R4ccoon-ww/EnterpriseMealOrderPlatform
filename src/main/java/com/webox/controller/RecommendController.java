package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.dto.RecommendRequest;
import com.webox.dto.RecommendResponse;
import com.webox.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @PostMapping
    public ApiResponse<RecommendResponse> recommend(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                                    @Valid @RequestBody RecommendRequest req) {
        return ApiResponse.ok(recommendService.recommend(userId, req.getQuery()));
    }
}
