package com.webox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.common.BizException;
import com.webox.dto.PreferenceRequest;
import com.webox.entity.UserPreference;
import com.webox.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreference get(Long userId) {
        UserPreference pref = userPreferenceMapper.findByUserId(userId);
        if (pref == null) {
            // 未设置时返回空偏好而非 404，便于前端直接渲染表单
            pref = new UserPreference();
            pref.setUserId(userId);
            pref.setAllergens("[]");
            pref.setPreferredCategories("[]");
        }
        return pref;
    }

    public UserPreference save(Long userId, PreferenceRequest req) {
        if (req.getBudgetMin() != null && req.getBudgetMax() != null
                && req.getBudgetMin().compareTo(req.getBudgetMax()) > 0) {
            throw new BizException("价格区间下限不能大于上限");
        }
        UserPreference pref = new UserPreference();
        pref.setUserId(userId);
        pref.setAllergens(toJson(req.getAllergens()));
        pref.setPreferredCategories(toJson(req.getPreferredCategories()));
        pref.setSpicyLevel(req.getSpicyLevel());
        pref.setTaste(req.getTaste());
        pref.setBudgetMin(req.getBudgetMin());
        pref.setBudgetMax(req.getBudgetMax());
        userPreferenceMapper.upsert(pref);
        return userPreferenceMapper.findByUserId(userId);
    }

    private String toJson(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list == null ? Collections.emptyList() : list);
        } catch (JsonProcessingException e) {
            throw new BizException("偏好数据格式错误");
        }
    }
}
