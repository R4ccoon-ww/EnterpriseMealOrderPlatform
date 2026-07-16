package com.webox.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class CreateOrderRequest {

    /** 格式 YYYY-MM-DD，缺省为当天 */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "配送日期格式应为 YYYY-MM-DD")
    private String deliveryDate;

    @NotBlank(message = "餐次不能为空")
    @Pattern(regexp = "^(lunch|dinner)$", message = "餐次只能是 lunch 或 dinner")
    private String mealPeriod;

    @NotBlank(message = "配送地址不能为空")
    private String deliveryAddress;
}
