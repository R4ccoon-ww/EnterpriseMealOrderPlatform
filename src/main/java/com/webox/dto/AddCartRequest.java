package com.webox.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AddCartRequest {

    @NotBlank(message = "菜品 ID 不能为空")
    private String menuItemId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为 1")
    @Max(value = 99, message = "单菜品数量不能超过 99")
    private Integer quantity = 1;
}
