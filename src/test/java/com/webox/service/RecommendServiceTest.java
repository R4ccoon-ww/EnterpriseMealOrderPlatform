package com.webox.service;

import com.webox.dto.RecommendResponse;
import com.webox.entity.MenuItem;
import com.webox.entity.UserPreference;
import com.webox.mapper.MenuItemMapper;
import com.webox.mapper.UserPreferenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private MenuItemMapper menuItemMapper;
    @Mock
    private UserPreferenceMapper userPreferenceMapper;
    @Mock
    private RestTemplate restTemplate;

    private RecommendService recommendService;

    @BeforeEach
    void setUp() {
        recommendService = new RecommendService(menuItemMapper, userPreferenceMapper, restTemplate);
        // 默认不配置 API Key → 走规则引擎
        ReflectionTestUtils.setField(recommendService, "apiKey", "");
        ReflectionTestUtils.setField(recommendService, "model", "claude-opus-4-8");
        ReflectionTestUtils.setField(recommendService, "baseUrl", "https://api.anthropic.com");
    }

    private static MenuItem item(String id, String name, String desc, String category, String price, String allergens) {
        MenuItem i = new MenuItem();
        i.setId(id);
        i.setName(name);
        i.setDescription(desc);
        i.setCategory(category);
        i.setPrice(new BigDecimal(price));
        i.setAllergens(allergens);
        i.setAvailable(true);
        return i;
    }

    /** 与种子数据一致的完整 8 个菜品 */
    private List<MenuItem> fullMenu() {
        return Arrays.asList(
                item("item_001", "宫保鸡丁", "经典川菜，鸡肉搭配花生、干辣椒爆炒", "chinese", "22", "[\"peanut\"]"),
                item("item_002", "凯撒沙拉", "新鲜罗马生菜配帕玛森芝士与凯撒酱", "salad", "28", "[\"dairy\",\"egg\"]"),
                item("item_003", "三文鱼刺身定食", "新鲜三文鱼刺身搭配米饭、味噌汤", "japanese", "45", "[\"fish\"]"),
                item("item_004", "番茄意面", "经典意式番茄酱意大利面配新鲜罗勒", "western", "26", "[\"gluten\"]"),
                item("item_005", "冬阴功汤", "泰式酸辣虾汤配香茅、南姜、柠檬叶", "southeast_asian", "32", "[\"shellfish\"]"),
                item("item_006", "鸡胸肉藜麦碗", "低脂高蛋白，烤鸡胸配藜麦、牛油果、时蔬", "salad", "35", "[]"),
                item("item_007", "麻婆豆腐", "四川经典，嫩豆腐配麻辣肉末", "chinese", "18", "[\"soy\"]"),
                item("item_008", "韩式拌饭", "石锅拌饭配各式时蔬、煎蛋与辣酱", "korean", "30", "[\"egg\",\"soy\"]"));
    }

    @Test
    void ruleEngineMatchesLightFood() {
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);

        RecommendResponse resp = recommendService.recommend(1L, "想吃清淡的");

        assertEquals("rule", resp.getSource());
        assertTrue(resp.getRecommendations().size() >= 3 && resp.getRecommendations().size() <= 5);
        // 清淡 → salad 类目应排在推荐首位
        String firstCategory = resp.getRecommendations().get(0).getMenuItem().getCategory();
        assertEquals("salad", firstCategory);
    }

    @Test
    void ruleEngineMatchesJapanese() {
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);

        RecommendResponse resp = recommendService.recommend(1L, "今天想吃日料");
        assertEquals("item_003", resp.getRecommendations().get(0).getMenuItem().getId());
    }

    @Test
    void ruleEngineExcludesUserAllergens() {
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        UserPreference pref = new UserPreference();
        pref.setAllergens("[\"peanut\"]");
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(pref);

        RecommendResponse resp = recommendService.recommend(1L, "随便来点");
        List<String> ids = resp.getRecommendations().stream()
                .map(r -> r.getMenuItem().getId()).collect(Collectors.toList());
        assertFalse(ids.contains("item_001"), "含花生的菜不应出现在推荐里");
    }

    @Test
    void unmatchedQueryStillReturnsAtLeastThree() {
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);

        RecommendResponse resp = recommendService.recommend(1L, "xyz 无关键词");
        assertTrue(resp.getRecommendations().size() >= 3, "无命中时用兜底补齐 3 个");
    }

    @Test
    void noApiKeySkipsLlmCall() {
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);

        recommendService.recommend(1L, "想吃辣的");
        verify(restTemplate, never()).postForObject(any(String.class), any(), any());
    }

    @Test
    void llmFailureFallsBackToRules() {
        ReflectionTestUtils.setField(recommendService, "apiKey", "sk-test");
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);
        when(restTemplate.postForObject(any(String.class), any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

        RecommendResponse resp = recommendService.recommend(1L, "想吃清淡的");
        assertEquals("rule", resp.getSource(), "LLM 调用失败应降级为规则引擎");
        assertFalse(resp.getRecommendations().isEmpty());
    }

    @Test
    void llmSuccessParsesRecommendations() {
        ReflectionTestUtils.setField(recommendService, "apiKey", "sk-test");
        when(menuItemMapper.findAvailable(null)).thenReturn(fullMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);
        String llmResponse = "{\"content\":[{\"type\":\"text\",\"text\":"
                + "\"[{\\\"id\\\":\\\"item_006\\\",\\\"reason\\\":\\\"高蛋白低脂\\\"},"
                + "{\\\"id\\\":\\\"item_002\\\",\\\"reason\\\":\\\"清爽沙拉\\\"}]\"}]}";
        when(restTemplate.postForObject(any(String.class), any(), any())).thenReturn(llmResponse);

        RecommendResponse resp = recommendService.recommend(1L, "健身餐");

        assertEquals("llm", resp.getSource());
        assertEquals(2, resp.getRecommendations().size());
        assertEquals("item_006", resp.getRecommendations().get(0).getMenuItem().getId());
        assertEquals("高蛋白低脂", resp.getRecommendations().get(0).getReason());
    }
}
