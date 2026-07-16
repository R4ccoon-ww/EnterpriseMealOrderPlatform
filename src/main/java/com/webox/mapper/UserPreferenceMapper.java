package com.webox.mapper;

import com.webox.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPreferenceMapper {

    UserPreference findByUserId(@Param("userId") Long userId);

    int upsert(UserPreference preference);
}
