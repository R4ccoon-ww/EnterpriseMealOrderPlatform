package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.dto.CreateOrderRequest;
import com.webox.entity.Order;
import com.webox.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/success")
    public ApiResponse<Order> create(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                     @Valid @RequestBody CreateOrderRequest req) {
        return ApiResponse.ok(orderService.createFromCart(userId, req));
    }

    @GetMapping
    public ApiResponse<List<Order>> list(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId) {
        return ApiResponse.ok(orderService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                     @PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(userId, id));
    }
}
