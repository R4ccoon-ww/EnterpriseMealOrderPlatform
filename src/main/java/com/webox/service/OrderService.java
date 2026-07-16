package com.webox.service;

import com.webox.common.BizException;
import com.webox.dto.CreateOrderRequest;
import com.webox.entity.CartItem;
import com.webox.entity.Order;
import com.webox.entity.OrderItem;
import com.webox.mapper.CartItemMapper;
import com.webox.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemMapper cartItemMapper;
    private final OrderMapper orderMapper;

    /**
     * 从购物车生成订单：建单 + 菜品快照 + 清空购物车，整体事务。
     */
    @Transactional
    public Order createFromCart(Long userId, CreateOrderRequest req) {
        List<CartItem> cartItems = cartItemMapper.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BizException(4004, "购物车为空，无法下单");
        }

        BigDecimal total = cartItems.stream()
                .map(c -> c.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setDeliveryDate(req.getDeliveryDate() != null ? req.getDeliveryDate() : LocalDate.now().toString());
        order.setMealPeriod(req.getMealPeriod());
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setStatus("pending");
        orderMapper.insertOrder(order);

        List<OrderItem> orderItems = cartItems.stream().map(c -> {
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setMenuItemId(c.getMenuItemId());
            oi.setName(c.getName());
            oi.setPrice(c.getPrice());
            oi.setQuantity(c.getQuantity());
            return oi;
        }).collect(Collectors.toList());
        orderMapper.insertOrderItems(orderItems);

        cartItemMapper.deleteByUserId(userId);

        order.setItems(orderItems);
        return order;
    }

    public List<Order> listByUser(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    public Order getDetail(Long userId, Long orderId) {
        Order order = orderMapper.findByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        order.setItems(orderMapper.findItemsByOrderId(orderId));
        return order;
    }
}
