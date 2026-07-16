package com.webox.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PreferenceRequest {
    /** 需排除的过敏原，如 ["peanut","dairy"] */
    private List<String> allergens;
    /** 偏好菜系，如 ["chinese","japanese"] */
    private List<String> preferredCategories;
    /** none | mild | hot */
    private String spicyLevel;
    /** light | medium | heavy */
    private String taste;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
}
