package cn.nicenan.mahumall.cart.service;

import cn.nicenan.mahumall.cart.vo.Cart;
import cn.nicenan.mahumall.cart.vo.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.concurrent.ExecutionException;

public interface CartService {

    Cart getCart();

    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException, JsonProcessingException;

    CartItem getCartItem(Long skuId) throws JsonProcessingException;
}
