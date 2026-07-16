package com.webox.mapper;

import com.webox.entity.Order;
import com.webox.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insertOrder(Order order);

    int insertOrderItems(@Param("items") List<OrderItem> items);

    List<Order> findByUserId(@Param("userId") Long userId);

    Order findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<OrderItem> findItemsByOrderId(@Param("orderId") Long orderId);
}
