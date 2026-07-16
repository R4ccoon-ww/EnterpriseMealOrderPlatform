package com.webox.controller;

import com.webox.auth.JwtInterceptor;
import com.webox.common.ApiResponse;
import com.webox.dto.AddCartRequest;
import com.webox.dto.CartResponse;
import com.webox.service.CartService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId) {
        return ApiResponse.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> add(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                         @Valid @RequestBody AddCartRequest req) {
        return ApiResponse.ok(cartService.add(userId, req));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CartResponse> updateQuantity(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody UpdateQuantityRequest req) {
        return ApiResponse.ok(cartService.updateQuantity(userId, id, req.getQuantity()));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<CartResponse> remove(@RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
                                            @PathVariable Long id) {
        return ApiResponse.ok(cartService.remove(userId, id));
    }

    @Data
    public static class UpdateQuantityRequest {
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量最小为 1")
        @Max(value = 99, message = "单菜品数量不能超过 99")
        private Integer quantity;
    }
}
