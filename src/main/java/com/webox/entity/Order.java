package com.webox.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String deliveryDate;
    private String mealPeriod;
    private String deliveryAddress;
    private String status;
    private LocalDateTime createdAt;

    private List<OrderItem> items;
}
