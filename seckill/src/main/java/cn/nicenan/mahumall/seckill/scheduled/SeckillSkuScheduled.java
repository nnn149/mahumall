package cn.nicenan.mahumall.seckill.scheduled;

import cn.nicenan.mahumall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class SeckillSkuScheduled {
    @Autowired
    private SeckillService seckillService;

    @Autowired
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    /**
     * 这里应该是幂等的
     * 三秒执行一次：* /3 * * * * ?
     * 8小时执行一次：0 0 0-8 * * ?
     */
    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkuLatest3Day() {
        log.info("\n上架秒杀商品的信息");
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Day();
        } catch (Exception ex) {
            log.error("上架秒杀商品的信息失败", ex);
        } finally {
            lock.unlock();
        }

    }
}
