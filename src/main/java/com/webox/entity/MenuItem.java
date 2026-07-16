package com.webox.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
public class MenuItem {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private String category;
    /** 数据库中的 JSON 字符串，如 ["peanut","dairy"] */
    @JsonIgnore
    private String allergens;
    private Boolean available;

    /** 对外序列化为数组 */
    @com.fasterxml.jackson.annotation.JsonProperty("allergens")
    public List<String> getAllergenList() {
        if (allergens == null || allergens.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(allergens, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
