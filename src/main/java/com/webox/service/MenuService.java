package com.webox.service;

import com.webox.common.BizException;
import com.webox.entity.MenuItem;
import com.webox.entity.UserPreference;
import com.webox.mapper.MenuItemMapper;
import com.webox.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemMapper menuItemMapper;
    private final UserPreferenceMapper userPreferenceMapper;

    /**
     * @param userId 非 null 时按该用户偏好过滤过敏原/预算，并将偏好菜系排前（加分项 A）
     */
    public List<MenuItem> list(String category, Long userId) {
        List<MenuItem> items = menuItemMapper.findAvailable(category);
        if (userId == null) {
            return items;
        }
        UserPreference pref = userPreferenceMapper.findByUserId(userId);
        if (pref == null) {
            return items;
        }
        return applyPreference(items, pref);
    }

    public List<MenuItem> applyPreference(List<MenuItem> items, UserPreference pref) {
        Set<String> excluded = new HashSet<>(pref.getAllergenList());
        BigDecimal min = pref.getBudgetMin();
        BigDecimal max = pref.getBudgetMax();
        Set<String> preferredCats = new HashSet<>(pref.getPreferredCategoryList());

        return items.stream()
                .filter(i -> i.getAllergenList().stream().noneMatch(excluded::contains))
                .filter(i -> min == null || i.getPrice().compareTo(min) >= 0)
                .filter(i -> max == null || i.getPrice().compareTo(max) <= 0)
                .sorted(Comparator.comparing((MenuItem i) -> !preferredCats.contains(i.getCategory()))
                        .thenComparing(MenuItem::getId))
                .collect(Collectors.toList());
    }

    public MenuItem getById(String id) {
        MenuItem item = menuItemMapper.findById(id);
        if (item == null) {
            throw new BizException(404, "菜品不存在");
        }
        return item;
    }
}
