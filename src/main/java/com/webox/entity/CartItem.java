package com.webox.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItem {
    private Long id;
    private Long userId;
    private String menuItemId;
    private Integer quantity;
    private LocalDateTime createdAt;

    /** 联查菜品信息 */
    private String name;
    private BigDecimal price;
    private String image;
    private String category;
}
