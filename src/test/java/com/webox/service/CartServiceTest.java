package com.webox.service;

import com.webox.common.BizException;
import com.webox.dto.AddCartRequest;
import com.webox.dto.CartResponse;
import com.webox.entity.CartItem;
import com.webox.entity.MenuItem;
import com.webox.mapper.CartItemMapper;
import com.webox.mapper.MenuItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private MenuItemMapper menuItemMapper;

    @InjectMocks
    private CartService cartService;

    private static final Long USER = 1L;

    private static MenuItem menuItem(String id, boolean available) {
        MenuItem i = new MenuItem();
        i.setId(id);
        i.setAvailable(available);
        i.setPrice(new BigDecimal("22"));
        return i;
    }

    private static CartItem cartItem(Long id, String menuItemId, int qty, String price) {
        CartItem c = new CartItem();
        c.setId(id);
        c.setUserId(USER);
        c.setMenuItemId(menuItemId);
        c.setQuantity(qty);
        c.setPrice(new BigDecimal(price));
        return c;
    }

    @Test
    void getCartComputesTotals() {
        // 22*3 + 35*1 = 101，数量 4
        when(cartItemMapper.findByUserId(USER)).thenReturn(Arrays.asList(
                cartItem(1L, "item_001", 3, "22"),
                cartItem(2L, "item_006", 1, "35")));

        CartResponse resp = cartService.getCart(USER);
        assertEquals(new BigDecimal("101"), resp.getTotalAmount());
        assertEquals(4, resp.getTotalQuantity());
    }

    @Test
    void getCartEmptyIsZero() {
        when(cartItemMapper.findByUserId(USER)).thenReturn(Collections.emptyList());
        CartResponse resp = cartService.getCart(USER);
        assertEquals(BigDecimal.ZERO, resp.getTotalAmount());
        assertEquals(0, resp.getTotalQuantity());
    }

    @Test
    void addRejectsUnknownMenuItem() {
        when(menuItemMapper.findById("item_999")).thenReturn(null);
        AddCartRequest req = new AddCartRequest();
        req.setMenuItemId("item_999");
        req.setQuantity(1);

        BizException e = assertThrows(BizException.class, () -> cartService.add(USER, req));
        assertEquals(404, e.getCode());
        verify(cartItemMapper, never()).insert(any());
    }

    @Test
    void addRejectsUnavailableMenuItem() {
        when(menuItemMapper.findById("item_001")).thenReturn(menuItem("item_001", false));
        AddCartRequest req = new AddCartRequest();
        req.setMenuItemId("item_001");
        req.setQuantity(1);

        assertThrows(BizException.class, () -> cartService.add(USER, req));
    }

    @Test
    void addNewItemInserts() {
        when(menuItemMapper.findById("item_001")).thenReturn(menuItem("item_001", true));
        when(cartItemMapper.findByUserAndMenuItem(USER, "item_001")).thenReturn(null);
        when(cartItemMapper.findByUserId(USER)).thenReturn(Collections.emptyList());

        AddCartRequest req = new AddCartRequest();
        req.setMenuItemId("item_001");
        req.setQuantity(2);
        cartService.add(USER, req);

        verify(cartItemMapper).insert(org.mockito.ArgumentMatchers.argThat(c ->
                c.getQuantity() == 2 && "item_001".equals(c.getMenuItemId())));
    }

    @Test
    void addExistingItemAccumulatesQuantity() {
        when(menuItemMapper.findById("item_001")).thenReturn(menuItem("item_001", true));
        when(cartItemMapper.findByUserAndMenuItem(USER, "item_001"))
                .thenReturn(cartItem(9L, "item_001", 1, "22"));
        when(cartItemMapper.findByUserId(USER)).thenReturn(Collections.emptyList());

        AddCartRequest req = new AddCartRequest();
        req.setMenuItemId("item_001");
        req.setQuantity(2);
        cartService.add(USER, req);

        verify(cartItemMapper).updateQuantity(9L, 3); // 1 + 2
        verify(cartItemMapper, never()).insert(any());
    }

    @Test
    void updateQuantityRejectsBelowOne() {
        BizException e = assertThrows(BizException.class,
                () -> cartService.updateQuantity(USER, 1L, 0));
        assertEquals(4000, e.getCode());
    }

    @Test
    void updateQuantityRejectsOtherUsersItem() {
        when(cartItemMapper.findByIdAndUserId(1L, USER)).thenReturn(null);
        BizException e = assertThrows(BizException.class,
                () -> cartService.updateQuantity(USER, 1L, 2));
        assertEquals(404, e.getCode());
    }

    @Test
    void removeRejectsOtherUsersItem() {
        when(cartItemMapper.findByIdAndUserId(1L, USER)).thenReturn(null);
        assertThrows(BizException.class, () -> cartService.remove(USER, 1L));
        verify(cartItemMapper, never()).delete(any());
    }
}
