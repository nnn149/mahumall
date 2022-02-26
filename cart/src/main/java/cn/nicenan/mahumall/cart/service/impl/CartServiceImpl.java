package cn.nicenan.mahumall.cart.service.impl;

import cn.nicenan.mahumall.cart.feign.ProductFeignService;
import cn.nicenan.mahumall.cart.interceptor.CartInterceptor;
import cn.nicenan.mahumall.cart.interceptor.UserInfoTo;
import cn.nicenan.mahumall.cart.service.CartService;
import cn.nicenan.mahumall.cart.vo.Cart;
import cn.nicenan.mahumall.cart.vo.CartItem;
import cn.nicenan.mahumall.cart.vo.SkuInfoVo;
import cn.nicenan.mahumall.common.utils.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    ObjectMapper objectMapper;
    private final String CART_PREFIX = "FIRE:cart:";

    @Override
    public Cart getCart() {
        return new Cart();
    }

    /**
     * 获取到我们要操作的购物车 [已经包含用户前缀 只需要带上用户id 或者临时id 就能对购物车进行操作]
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        // 1. 这里我们需要知道操作的是离线购物车还是在线购物车
        String cartKey = CART_PREFIX;
        if (userInfoTo.getUserId() != null) {
            System.out.println("\n用户 [" + userInfoTo.getUsername() + "] 正在操作购物车");
            // 已登录的用户购物车的标识
            cartKey += userInfoTo.getUserId();
        } else {
            System.out.println("\n临时用户 [" + userInfoTo.getUserKey() + "] 正在操作购物车");
            // 未登录的用户购物车的标识
            cartKey += userInfoTo.getUserKey();
        }
        // 绑定这个 key 以后所有对redis 的操作都是针对这个key
        return stringRedisTemplate.boundHashOps(cartKey);
    }

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException, JsonProcessingException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            CartItem cartItem = new CartItem();
            // 异步编排
            CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
                // 1. 远程查询当前要添加的商品的信息
                R<SkuInfoVo> skuInfo = productFeignService.skuInfo(skuId);
                SkuInfoVo sku = skuInfo.getData(new TypeReference<>() {
                });
                // 2. 添加新商品到购物车
                cartItem.setCount(num);
                cartItem.setCheck(true);
                cartItem.setImage(sku.getSkuDefaultImg());
                cartItem.setPrice(sku.getPrice());
                cartItem.setTitle(sku.getSkuTitle());
                cartItem.setSkuId(skuId);
            }, executor);

            // 3. 远程查询sku组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);
            CompletableFuture.allOf(getSkuInfo, getSkuSaleAttrValues).get();
            cartOps.put(skuId.toString(), objectMapper.writeValueAsString(cartItem));
            return cartItem;
        } else {
            CartItem cartItem = objectMapper.readValue(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), objectMapper.writeValueAsString(cartItem));
            return cartItem;
        }
    }

    @Override
    public CartItem getCartItem(Long skuId) throws JsonProcessingException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = (String) cartOps.get(skuId.toString());
        return objectMapper.readValue(o, CartItem.class);
    }
}
