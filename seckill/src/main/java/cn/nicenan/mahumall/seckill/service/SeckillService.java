package cn.nicenan.mahumall.seckill.service;

public interface SeckillService {

    void uploadSeckillSkuLatest3Day();

    /**
     * 获取当前可以参与秒杀的商品信息
     */
//    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

//    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
