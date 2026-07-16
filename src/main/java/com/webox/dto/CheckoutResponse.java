package com.webox.dto;

import com.webox.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认页预览：购物车内容 + 配送信息 + 价格汇总，不落库。
 */
@Data
@AllArgsConstructor
public class CheckoutResponse {

    /** 购物车商品列表（含单价、数量、小计） */
    private List<CartItem> items;
    /** 总金额 */
    private BigDecimal totalAmount;
    /** 总件数 */
    private int totalQuantity;
    /** 配送日期 YYYY-MM-DD（来自请求或默认当天） */
    private String deliveryDate;
    /** lunch | dinner */
    private String mealPeriod;
    /** 配送地址 */
    private String deliveryAddress;
}
