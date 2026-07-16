package com.webox.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PreferenceRequest {

    private List<String> allergens;
    private List<String> preferredCategories;

    @Pattern(regexp = "^(none|mild|hot)$", message = "辣度只能为 none/mild/hot")
    private String spicyLevel;

    @Pattern(regexp = "^(light|medium|heavy)$", message = "口味只能为 light/medium/heavy")
    private String taste;

    @DecimalMin(value = "0", message = "预算下限不能为负数")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0", message = "预算上限不能为负数")
    private BigDecimal budgetMax;
}
