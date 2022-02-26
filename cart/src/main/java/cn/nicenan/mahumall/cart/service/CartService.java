package cn.nicenan.mahumall.cart.service;

import cn.nicenan.mahumall.cart.vo.Cart;
import cn.nicenan.mahumall.cart.vo.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {

    Cart getCart() throws ExecutionException, InterruptedException, JsonProcessingException;

    void clearCart(String cartKey);

    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException, JsonProcessingException;

    CartItem getCartItem(Long skuId) throws JsonProcessingException;

    /**
     * 勾选购物项
     */
    void checkItem(Long skuId, Integer check) throws JsonProcessingException;

    /**
     * 改变购物车中物品的数量
     */
    void changeItemCount(Long skuId, Integer num) throws JsonProcessingException;

    /**
     * 删除购物项
     */
    void deleteItem(Long skuId);

    /**
     * 结账
     */
    BigDecimal toTrade() throws ExecutionException, InterruptedException, JsonProcessingException;

    List<CartItem> getUserCartItems();
}
