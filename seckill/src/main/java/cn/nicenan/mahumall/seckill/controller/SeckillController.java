package cn.nicenan.mahumall.seckill.controller;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.seckill.service.SeckillService;
import cn.nicenan.mahumall.seckill.to.SeckillSkuRedisTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R<List<SeckillSkuRedisTo>> getCurrentSeckillSkus() {
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R<SeckillSkuRedisTo> getSkuSeckillInfo(@PathVariable("skuId") Long skuId) throws JsonProcessingException {
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }



    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId, @RequestParam("key") String key, @RequestParam("num") Integer num, Model model) throws JsonProcessingException {
        String orderSn = seckillService.kill(killId,key,num);
        // 1.判断是否登录
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
