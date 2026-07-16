package com.webox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.webox.dto.RecommendResponse;
import com.webox.dto.RecommendResponse.Recommendation;
import com.webox.entity.MenuItem;
import com.webox.entity.UserPreference;
import com.webox.mapper.MenuItemMapper;
import com.webox.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 推荐（加分项 B）：
 * 配置了 ANTHROPIC_API_KEY 时调用 Claude Messages API 做自然语言推荐；
 * 未配置或调用失败时自动降级为关键词规则引擎，保证本地测试不依赖外部服务。
 * 两条路径都排除用户偏好中设置的过敏原（偏好联动）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MIN_RESULTS = 3;
    private static final int MAX_RESULTS = 5;

    private final MenuItemMapper menuItemMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final RestTemplate restTemplate;

    @Value("${webox.ai.api-key}")
    private String apiKey;

    @Value("${webox.ai.model}")
    private String model;

    @Value("${webox.ai.base-url}")
    private String baseUrl;

    public RecommendResponse recommend(Long userId, String query) {
        UserPreference pref = userPreferenceMapper.findByUserId(userId);
        Set<String> excludedAllergens = pref == null
                ? new HashSet<>() : new HashSet<>(pref.getAllergenList());

        // 候选菜单：当日可订且排除用户过敏原
        List<MenuItem> candidates = menuItemMapper.findAvailable(null).stream()
                .filter(i -> i.getAllergenList().stream().noneMatch(excludedAllergens::contains))
                .collect(Collectors.toList());

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                return recommendByLlm(query, pref, candidates);
            } catch (Exception e) {
                log.warn("LLM 推荐失败，降级为规则引擎: {}", e.getMessage());
            }
        }
        return recommendByRules(query, candidates);
    }

    // ==================== LLM 路径 ====================

    private RecommendResponse recommendByLlm(String query, UserPreference pref, List<MenuItem> candidates) throws Exception {
        String menuJson = MAPPER.writeValueAsString(candidates.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("name", i.getName());
            m.put("description", i.getDescription());
            m.put("price", i.getPrice());
            m.put("category", i.getCategory());
            m.put("allergens", i.getAllergenList());
            return m;
        }).collect(Collectors.toList()));

        String prefText = pref == null ? "无" :
                String.format("偏好菜系=%s, 辣度=%s, 口味=%s, 预算=%s~%s",
                        pref.getPreferredCategoryList(), pref.getSpicyLevel(), pref.getTaste(),
                        pref.getBudgetMin(), pref.getBudgetMax());

        String prompt = "你是企业餐食订购平台的推荐助手。根据用户的自然语言需求和偏好，从当日菜单中推荐 3-5 个菜品。\n"
                + "用户需求：" + query + "\n"
                + "用户偏好：" + prefText + "\n"
                + "当日菜单（已排除用户过敏原）：" + menuJson + "\n"
                + "只返回 JSON 数组，不要其他文字，格式：[{\"id\":\"item_001\",\"reason\":\"推荐理由\"}]。"
                + "id 必须来自上面的菜单。";

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        String response = restTemplate.postForObject(
                baseUrl + "/v1/messages", new HttpEntity<>(body.toString(), headers), String.class);

        JsonNode root = MAPPER.readTree(response);
        String text = root.path("content").path(0).path("text").asText();
        // 容错：截取文本中的 JSON 数组部分
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new IllegalStateException("LLM 返回内容不含 JSON 数组");
        }
        JsonNode arr = MAPPER.readTree(text.substring(start, end + 1));

        Map<String, MenuItem> byId = candidates.stream()
                .collect(Collectors.toMap(MenuItem::getId, i -> i));
        List<Recommendation> recs = new ArrayList<>();
        for (JsonNode node : arr) {
            MenuItem item = byId.get(node.path("id").asText());
            if (item != null && recs.size() < MAX_RESULTS) {
                recs.add(new Recommendation(item, node.path("reason").asText("符合你的需求")));
            }
        }
        if (recs.isEmpty()) {
            throw new IllegalStateException("LLM 未返回有效菜品");
        }
        return new RecommendResponse("llm", recs);
    }

    // ==================== 规则引擎降级路径 ====================

    /** 关键词 -> 打分规则。命中一条关键词即对匹配的菜品加分。 */
    private RecommendResponse recommendByRules(String query, List<MenuItem> candidates) {
        Map<String, Integer> scores = new HashMap<>();
        Map<String, List<String>> reasons = new HashMap<>();

        for (MenuItem item : candidates) {
            scores.put(item.getId(), 0);
            reasons.put(item.getId(), new ArrayList<>());
        }

        applyRule(query, Arrays.asList("清淡", "轻食", "健康", "少油"), candidates, scores, reasons,
                i -> "salad".equals(i.getCategory()) || i.getDescription().contains("低脂")
                        || i.getName().contains("沙拉"), "口味清淡健康");
        applyRule(query, Arrays.asList("高蛋白", "低脂", "健身", "减脂", "蛋白"), candidates, scores, reasons,
                i -> i.getDescription().contains("高蛋白") || i.getDescription().contains("低脂")
                        || i.getDescription().contains("鸡胸"), "高蛋白低脂");
        applyRule(query, Arrays.asList("辣", "麻辣", "川菜", "重口"), candidates, scores, reasons,
                i -> i.getDescription().contains("辣") || i.getDescription().contains("川菜")
                        || i.getDescription().contains("麻"), "香辣过瘾");
        applyRule(query, Arrays.asList("日料", "日本", "刺身", "寿司"), candidates, scores, reasons,
                i -> "japanese".equals(i.getCategory()), "地道日式风味");
        applyRule(query, Arrays.asList("中餐", "中式", "家常"), candidates, scores, reasons,
                i -> "chinese".equals(i.getCategory()), "经典中餐");
        applyRule(query, Arrays.asList("西餐", "意面", "披萨", "沙拉"), candidates, scores, reasons,
                i -> "western".equals(i.getCategory()) || "salad".equals(i.getCategory()), "西式风味");
        applyRule(query, Arrays.asList("汤", "热汤", "暖胃"), candidates, scores, reasons,
                i -> i.getName().contains("汤") || i.getDescription().contains("汤"), "热汤暖胃");
        applyRule(query, Arrays.asList("便宜", "实惠", "省钱", "性价比"), candidates, scores, reasons,
                i -> i.getPrice().doubleValue() <= 25, "价格实惠");
        applyRule(query, Arrays.asList("韩式", "韩国", "拌饭"), candidates, scores, reasons,
                i -> "korean".equals(i.getCategory()), "韩式风味");
        applyRule(query, Arrays.asList("泰式", "东南亚", "酸辣"), candidates, scores, reasons,
                i -> "southeast_asian".equals(i.getCategory()), "东南亚风味");

        List<MenuItem> ranked = candidates.stream()
                .sorted((a, b) -> scores.get(b.getId()) - scores.get(a.getId()))
                .collect(Collectors.toList());

        List<Recommendation> recs = new ArrayList<>();
        for (MenuItem item : ranked) {
            if (recs.size() >= MAX_RESULTS) {
                break;
            }
            boolean matched = scores.get(item.getId()) > 0;
            // 命中的优先；不足 3 个时用未命中的补齐
            if (matched || recs.size() < MIN_RESULTS) {
                String reason = matched
                        ? String.join("、", reasons.get(item.getId()))
                        : "今日菜单精选，符合你的偏好";
                recs.add(new Recommendation(item, reason));
            }
        }
        return new RecommendResponse("rule", recs);
    }

    private void applyRule(String query, List<String> keywords, List<MenuItem> candidates,
                           Map<String, Integer> scores, Map<String, List<String>> reasons,
                           java.util.function.Predicate<MenuItem> matcher, String reason) {
        if (keywords.stream().noneMatch(query::contains)) {
            return;
        }
        for (MenuItem item : candidates) {
            if (matcher.test(item)) {
                scores.merge(item.getId(), 10, Integer::sum);
                reasons.get(item.getId()).add(reason);
            }
        }
    }
}
