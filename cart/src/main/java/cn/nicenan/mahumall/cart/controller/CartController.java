package cn.nicenan.mahumall.cart.controller;

import cn.nicenan.mahumall.cart.service.CartService;
import cn.nicenan.mahumall.cart.vo.Cart;
import cn.nicenan.mahumall.cart.vo.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;


@Controller
public class CartController {
    @Autowired
    CartService cartService;

    /**
     * 浏览器有一个cookie：user-key 标识用户身份 一个月后过期
     * 每次访问都会带上这个 user-key
     * 如果没有临时用户 还要帮忙创建一个
     */
    @GetMapping({"/", "/cart.html"})
    public String carListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * RedirectAttributes: 会自动将数据添加到url后面
     */
    @GetMapping("/addCartItem")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException, JsonProcessingException {
        cartService.addToCart(skuId, num);
        redirectAttributes.addAttribute("skuId", skuId);
        // 重定向到成功页面
        return "redirect:http://cart.mahumall.com/addToCartSuccess.html";
    }


    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam(value = "skuId", required = false) Object skuId, Model model) {
        CartItem cartItem = null;
        // 然后在查一遍 购物车
        if (skuId == null) {
            model.addAttribute("item", null);
        } else {
            try {
                cartItem = cartService.getCartItem(Long.parseLong((String) skuId));
            } catch (NumberFormatException e) {
                System.out.println("恶意操作! 页面传来非法字符.");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            model.addAttribute("item", cartItem);
        }
        return "success";
    }
}
