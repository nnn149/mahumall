package cn.nicenan.mahumall.seckill.service;

import cn.nicenan.mahumall.seckill.to.SeckillSkuRedisTo;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface SeckillService {

    void uploadSeckillSkuLatest3Day();

    /**
     * 获取当前可以参与秒杀的商品信息
     */
//    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

//    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num) throws JsonProcessingException;

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) throws JsonProcessingException;
}
