package com.webox.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class UserPreference {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long userId;
    /** JSON 字符串存储 */
    @JsonIgnore
    private String allergens;
    @JsonIgnore
    private String preferredCategories;
    private String spicyLevel;
    private String taste;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("allergens")
    public List<String> getAllergenList() {
        return parse(allergens);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("preferredCategories")
    public List<String> getPreferredCategoryList() {
        return parse(preferredCategories);
    }

    private static List<String> parse(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
