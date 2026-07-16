package com.webox.dto;

import com.webox.entity.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecommendResponse {

    /** "llm" 或 "rule" — 标识本次推荐由哪条路径产生 */
    private String source;
    private List<Recommendation> recommendations;

    @Data
    @AllArgsConstructor
    public static class Recommendation {
        private MenuItem menuItem;
        private String reason;
    }
}
