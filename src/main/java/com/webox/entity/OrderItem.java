package com.webox.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private String menuItemId;
    /** 下单时快照 */
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
