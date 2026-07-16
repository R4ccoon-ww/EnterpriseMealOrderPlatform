package com.webox.mapper;

import com.webox.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findById(@Param("id") Long id);

    int insert(User user);
}
