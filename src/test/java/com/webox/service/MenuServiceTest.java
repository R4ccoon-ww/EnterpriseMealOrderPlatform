package com.webox.service;

import com.webox.common.BizException;
import com.webox.entity.MenuItem;
import com.webox.entity.UserPreference;
import com.webox.mapper.MenuItemMapper;
import com.webox.mapper.UserPreferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuItemMapper menuItemMapper;
    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @InjectMocks
    private MenuService menuService;

    private static MenuItem item(String id, String category, String price, String allergensJson) {
        MenuItem i = new MenuItem();
        i.setId(id);
        i.setCategory(category);
        i.setPrice(new BigDecimal(price));
        i.setAllergens(allergensJson);
        i.setAvailable(true);
        return i;
    }

    /** 对应种子数据的关键子集：宫保鸡丁(花生)、凯撒沙拉、三文鱼定食(45)、鸡胸藜麦碗 */
    private List<MenuItem> sampleMenu() {
        return Arrays.asList(
                item("item_001", "chinese", "22", "[\"peanut\"]"),
                item("item_002", "salad", "28", "[\"dairy\",\"egg\"]"),
                item("item_003", "japanese", "45", "[\"fish\"]"),
                item("item_006", "salad", "35", "[]"));
    }

    private UserPreference pref(String allergens, String cats, String min, String max) {
        UserPreference p = new UserPreference();
        p.setAllergens(allergens);
        p.setPreferredCategories(cats);
        if (min != null) {
            p.setBudgetMin(new BigDecimal(min));
        }
        if (max != null) {
            p.setBudgetMax(new BigDecimal(max));
        }
        return p;
    }

    @Test
    void listWithoutUserReturnsAll() {
        when(menuItemMapper.findAvailable(null)).thenReturn(sampleMenu());
        assertEquals(4, menuService.list(null, null).size());
    }

    @Test
    void listWithUserButNoPreferenceReturnsAll() {
        when(menuItemMapper.findAvailable(null)).thenReturn(sampleMenu());
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);
        assertEquals(4, menuService.list(null, 1L).size());
    }

    @Test
    void applyPreferenceFiltersAllergens() {
        List<String> ids = menuService.applyPreference(sampleMenu(), pref("[\"peanut\"]", "[]", null, null))
                .stream().map(MenuItem::getId).collect(Collectors.toList());
        assertFalse(ids.contains("item_001"), "含花生的宫保鸡丁应被过滤");
        assertEquals(3, ids.size());
    }

    @Test
    void applyPreferenceFiltersBudget() {
        List<String> ids = menuService.applyPreference(sampleMenu(), pref("[]", "[]", "25", "40"))
                .stream().map(MenuItem::getId).collect(Collectors.toList());
        assertFalse(ids.contains("item_001"), "22 低于预算下限应被过滤");
        assertFalse(ids.contains("item_003"), "45 超出预算上限应被过滤");
        assertEquals(Arrays.asList("item_002", "item_006"), ids);
    }

    @Test
    void applyPreferenceSortsPreferredCategoryFirst() {
        List<String> ids = menuService.applyPreference(sampleMenu(), pref("[]", "[\"salad\"]", null, null))
                .stream().map(MenuItem::getId).collect(Collectors.toList());
        assertEquals(Arrays.asList("item_002", "item_006"), ids.subList(0, 2), "偏好菜系应排最前");
        assertTrue(ids.containsAll(Arrays.asList("item_001", "item_003")), "非偏好菜系仍保留");
    }

    @Test
    void getByIdThrows404WhenMissing() {
        when(menuItemMapper.findById("item_999")).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> menuService.getById("item_999"));
        assertEquals(404, e.getCode());
    }
}
