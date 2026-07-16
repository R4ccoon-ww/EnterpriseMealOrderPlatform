package com.webox.service;

import com.webox.common.BizException;
import com.webox.dto.AddCartRequest;
import com.webox.dto.CartResponse;
import com.webox.entity.CartItem;
import com.webox.entity.MenuItem;
import com.webox.mapper.CartItemMapper;
import com.webox.mapper.MenuItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final MenuItemMapper menuItemMapper;

    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemMapper.findByUserId(userId);
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int quantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        return new CartResponse(items, total, quantity);
    }

    @Transactional
    public CartResponse add(Long userId, AddCartRequest req) {
        MenuItem menuItem = menuItemMapper.findById(req.getMenuItemId());
        if (menuItem == null || !Boolean.TRUE.equals(menuItem.getAvailable())) {
            throw new BizException(404, "菜品不存在或已下架");
        }
        CartItem existing = cartItemMapper.findByUserAndMenuItem(userId, req.getMenuItemId());
        if (existing != null) {
            // 重复添加：数量累加
            cartItemMapper.updateQuantity(existing.getId(), existing.getQuantity() + req.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setMenuItemId(req.getMenuItemId());
            item.setQuantity(req.getQuantity());
            cartItemMapper.insert(item);
        }
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity < 1) {
            throw new BizException("数量最小为 1");
        }
        requireOwned(userId, cartItemId);
        cartItemMapper.updateQuantity(cartItemId, quantity);
        return getCart(userId);
    }

    @Transactional
    public CartResponse remove(Long userId, Long cartItemId) {
        requireOwned(userId, cartItemId);
        cartItemMapper.delete(cartItemId);
        return getCart(userId);
    }

    private void requireOwned(Long userId, Long cartItemId) {
        if (cartItemMapper.findByIdAndUserId(cartItemId, userId) == null) {
            throw new BizException(404, "购物车项不存在");
        }
    }
}
