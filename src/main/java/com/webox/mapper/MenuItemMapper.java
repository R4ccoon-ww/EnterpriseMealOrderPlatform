package com.webox.mapper;

import com.webox.entity.MenuItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuItemMapper {

    List<MenuItem> findAvailable(@Param("category") String category);

    MenuItem findById(@Param("id") String id);
}
