package com.webox.service;

import com.webox.common.BizException;
import com.webox.dto.CheckoutResponse;
import com.webox.dto.CreateOrderRequest;
import com.webox.entity.CartItem;
import com.webox.entity.Order;
import com.webox.entity.OrderItem;
import com.webox.mapper.CartItemMapper;
import com.webox.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER = 1L;

    private static CartItem cartItem(String menuItemId, String name, String price, int qty) {
        CartItem c = new CartItem();
        c.setMenuItemId(menuItemId);
        c.setName(name);
        c.setPrice(new BigDecimal(price));
        c.setQuantity(qty);
        return c;
    }

    private static CreateOrderRequest req(String date, String period, String address) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setDeliveryDate(date);
        r.setMealPeriod(period);
        r.setDeliveryAddress(address);
        return r;
    }

    // ==================== Checkout ====================

    @Test
    void checkoutReturnsFullPreview() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(Arrays.asList(
                cartItem("item_001", "宫保鸡丁", "22", 5),
                cartItem("item_006", "鸡胸肉藜麦碗", "35", 1)));

        CheckoutResponse resp = orderService.checkout(USER, req("2026-07-17", "lunch", "3F-301"));

        assertEquals(new BigDecimal("145"), resp.getTotalAmount());
        assertEquals(6, resp.getTotalQuantity());
        assertEquals("2026-07-17", resp.getDeliveryDate());
        assertEquals("lunch", resp.getMealPeriod());
        assertEquals("3F-301", resp.getDeliveryAddress());
        assertEquals(2, resp.getItems().size());
        assertEquals("宫保鸡丁", resp.getItems().get(0).getName());
    }

    @Test
    void checkoutFromEmptyCartRejected() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(Collections.emptyList());

        BizException e = assertThrows(BizException.class,
                () -> orderService.checkout(USER, req(null, "dinner", "X")));
        assertEquals(4004, e.getCode());
    }

    @Test
    void checkoutDefaultsDeliveryDateToToday() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(
                Collections.singletonList(cartItem("item_001", "宫保鸡丁", "22", 1)));

        CheckoutResponse resp = orderService.checkout(USER, req(null, "dinner", "3F"));

        assertEquals(LocalDate.now().toString(), resp.getDeliveryDate());
    }

    // ==================== Create Order ====================

    @Test
    void createFromEmptyCartRejected() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(Collections.emptyList());

        BizException e = assertThrows(BizException.class,
                () -> orderService.createFromCart(USER, req(null, "lunch", "3F")));
        assertEquals(4004, e.getCode());
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void createComputesTotalAndSnapshotsItems() {
        // 22*5 + 35*1 = 145
        when(cartItemMapper.findByUserId(USER)).thenReturn(Arrays.asList(
                cartItem("item_001", "宫保鸡丁", "22", 5),
                cartItem("item_006", "鸡胸肉藜麦碗", "35", 1)));

        Order order = orderService.createFromCart(USER, req("2026-07-17", "lunch", "3F-301"));

        assertEquals(new BigDecimal("145"), order.getTotalAmount());
        assertEquals("pending", order.getStatus());
        assertEquals("2026-07-17", order.getDeliveryDate());
        assertEquals(2, order.getItems().size());
        assertEquals("宫保鸡丁", order.getItems().get(0).getName());
        assertEquals(new BigDecimal("22"), order.getItems().get(0).getPrice());
        verify(orderMapper).insertOrderItems(anyList());
        verify(cartItemMapper).deleteByUserId(USER);
    }

    @Test
    void createDefaultsDeliveryDateToToday() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(
                Collections.singletonList(cartItem("item_001", "宫保鸡丁", "22", 1)));

        Order order = orderService.createFromCart(USER, req(null, "dinner", "3F"));
        assertEquals(LocalDate.now().toString(), order.getDeliveryDate());
    }

    @Test
    void listByUserReturnsOrders() {
        Order o1 = new Order(); o1.setId(1L); o1.setTotalAmount(new BigDecimal("145"));
        Order o2 = new Order(); o2.setId(2L); o2.setTotalAmount(new BigDecimal("28"));
        when(orderMapper.findByUserId(USER)).thenReturn(Arrays.asList(o1, o2));

        List<Order> orders = orderService.listByUser(USER);
        assertEquals(2, orders.size());
        assertEquals(1L, orders.get(0).getId());
        assertEquals(2L, orders.get(1).getId());
    }

    @Test
    void listByUserReturnsEmptyWhenNoOrders() {
        when(orderMapper.findByUserId(USER)).thenReturn(Collections.emptyList());

        List<Order> orders = orderService.listByUser(USER);
        assertEquals(0, orders.size());
    }

    @Test
    void getDetailReturnsOrderWithItems() {
        Order order = new Order();
        order.setId(10L);
        order.setTotalAmount(new BigDecimal("88"));
        order.setStatus("pending");
        when(orderMapper.findByIdAndUserId(10L, USER)).thenReturn(order);

        OrderItem oi = new OrderItem();
        oi.setId(1L); oi.setOrderId(10L); oi.setName("凯撒沙拉"); oi.setPrice(new BigDecimal("28")); oi.setQuantity(2);
        when(orderMapper.findItemsByOrderId(10L)).thenReturn(Collections.singletonList(oi));

        Order result = orderService.getDetail(USER, 10L);
        assertEquals(10L, result.getId());
        assertEquals(1, result.getItems().size());
        assertEquals("凯撒沙拉", result.getItems().get(0).getName());
    }

    @Test
    void getDetailRejectsOtherUsersOrder() {
        when(orderMapper.findByIdAndUserId(99L, USER)).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> orderService.getDetail(USER, 99L));
        assertEquals(404, e.getCode());
    }
}
