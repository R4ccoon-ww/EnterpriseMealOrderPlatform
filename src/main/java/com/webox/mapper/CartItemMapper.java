package com.webox.mapper;

import com.webox.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    List<CartItem> findByUserId(@Param("userId") Long userId);

    CartItem findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    CartItem findByUserAndMenuItem(@Param("userId") Long userId, @Param("menuItemId") String menuItemId);

    int insert(CartItem item);

    int updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    int delete(@Param("id") Long id);

    int deleteByUserId(@Param("userId") Long userId);
}
