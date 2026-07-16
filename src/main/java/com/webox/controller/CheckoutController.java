package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.dto.CheckoutResponse;
import com.webox.dto.CreateOrderRequest;
import com.webox.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<CheckoutResponse> checkout(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                                  @Valid @RequestBody CreateOrderRequest req) {
        return ApiResponse.ok(orderService.checkout(userId, req));
    }
}
