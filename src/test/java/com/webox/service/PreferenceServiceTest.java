package com.webox.service;

import com.webox.common.BizException;
import com.webox.dto.PreferenceRequest;
import com.webox.entity.UserPreference;
import com.webox.mapper.UserPreferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @InjectMocks
    private PreferenceService preferenceService;

    @Test
    void getReturnsEmptyDefaultWhenNotSet() {
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(null);

        UserPreference pref = preferenceService.get(1L);
        assertEquals(1L, pref.getUserId());
        assertTrue(pref.getAllergenList().isEmpty());
        assertTrue(pref.getPreferredCategoryList().isEmpty());
    }

    @Test
    void saveRejectsInvertedBudgetRange() {
        PreferenceRequest req = new PreferenceRequest();
        req.setBudgetMin(new BigDecimal("50"));
        req.setBudgetMax(new BigDecimal("20"));

        assertThrows(BizException.class, () -> preferenceService.save(1L, req));
        verify(userPreferenceMapper, never()).upsert(any());
    }

    @Test
    void saveSerializesListsToJson() {
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(new UserPreference());

        PreferenceRequest req = new PreferenceRequest();
        req.setAllergens(Arrays.asList("peanut", "dairy"));
        req.setPreferredCategories(Arrays.asList("salad"));
        preferenceService.save(1L, req);

        verify(userPreferenceMapper).upsert(org.mockito.ArgumentMatchers.argThat(p ->
                p.getAllergens().contains("peanut") && p.getAllergens().contains("dairy")
                        && p.getPreferredCategories().contains("salad")));
    }

    @Test
    void saveNullListsBecomeEmptyJsonArrays() {
        when(userPreferenceMapper.findByUserId(1L)).thenReturn(new UserPreference());

        preferenceService.save(1L, new PreferenceRequest());

        verify(userPreferenceMapper).upsert(org.mockito.ArgumentMatchers.argThat(p ->
                "[]".equals(p.getAllergens()) && "[]".equals(p.getPreferredCategories())));
    }
}
