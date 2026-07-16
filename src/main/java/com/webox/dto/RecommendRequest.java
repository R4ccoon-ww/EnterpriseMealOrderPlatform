package com.webox.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RecommendRequest {

    /** 用户自然语言，如 "想吃清淡的"、"来个高蛋白低脂的"、"今天想吃日料" */
    @NotBlank(message = "推荐请求内容不能为空")
    private String query;
}
