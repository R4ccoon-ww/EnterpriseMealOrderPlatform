package com.webox.dto;

import com.webox.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CartResponse {
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private int totalQuantity;
}
