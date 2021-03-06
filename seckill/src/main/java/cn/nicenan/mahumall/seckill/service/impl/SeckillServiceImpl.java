package cn.nicenan.mahumall.seckill.service.impl;

import cn.nicenan.mahumall.common.to.MemberRespTo;
import cn.nicenan.mahumall.common.to.mq.SecKillOrderTo;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.seckill.feign.CouponFeignService;
import cn.nicenan.mahumall.seckill.feign.ProductFeignService;
import cn.nicenan.mahumall.seckill.interceptor.LoginUserInterceptor;
import cn.nicenan.mahumall.seckill.service.SeckillService;
import cn.nicenan.mahumall.seckill.to.SeckillSkuRedisTo;
import cn.nicenan.mahumall.seckill.vo.SeckillSessionsWithSkus;
import cn.nicenan.mahumall.seckill.vo.SkuInfoVo;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.statement.Block;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

//    @Autowired
//    private RabbitTemplate rabbitTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

    private final String SKUSTOCK_SEMAPHONE = "seckill:stock:"; // +???????????????

    @Override
    public void uploadSeckillSkuLatest3Day() {
        // 1.??????????????????????????????????????????
        R<List<SeckillSessionsWithSkus>> r = couponFeignService.getLate3DaySession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> sessions = r.getData(new TypeReference<>() {
            });
            // 2.??????????????????
            saveSessionInfo(sessions);
            // 3.????????????????????????????????????
            saveSessionSkuInfo(sessions);
        }
    }

    /**
     * ??????????????????
     *
     * @param sessions
     */
    private void saveSessionInfo(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                long startTime = session.getStartTime().getTime();

                long endTime = session.getEndTime().getTime();
                String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (!hasKey) {
                    // ??????????????????id
                    List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "-" + item.getSkuId()).collect(Collectors.toList());
                    // ??????????????????
                    stringRedisTemplate.opsForList().leftPushAll(key, collect);
                }
            });
        }
    }

    @SuppressWarnings("AlibabaUndefineMagicConstant")
    private void saveSessionSkuInfo(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                    // 1.??????????????????
                    String randomCode = UUID.randomUUID().toString().replace("-", "");
                    if (Boolean.FALSE.equals(ops.hasKey(seckillSkuVo.getPromotionSessionId() + "-" + seckillSkuVo.getSkuId()))) {
                        // 2.????????????
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        BeanUtils.copyProperties(seckillSkuVo, redisTo);
                        // 3.sku??????????????? sku???????????????
                        R<SkuInfoVo> info = productFeignService.skuInfo(seckillSkuVo.getSkuId());
                        if (info.getCode() == 0) {
                            SkuInfoVo skuInfo = info.getData(new TypeReference<>() {
                            });
                            redisTo.setSkuInfoVo(skuInfo);
                        }
                        // 4.?????????????????????????????????
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());

                        redisTo.setRandomCode(randomCode);

                        try {
                            ops.put(seckillSkuVo.getPromotionSessionId() + "-" + seckillSkuVo.getSkuId(), new ObjectMapper().writeValueAsString(redisTo));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        // ????????????????????????????????????????????????????????????????????????
                        // 5.????????????????????????????????????  ??????
                        RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + randomCode);
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                    }
                });
            });
        }
    }

    @Override
    public String kill(String killId, String key, Integer num) throws JsonProcessingException {
        MemberRespTo memberRsepVo = LoginUserInterceptor.threadLocal.get();

        // 1.???????????????????????????????????????
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo redisTo = new ObjectMapper().readValue(json, SeckillSkuRedisTo.class);
            // ???????????????
            long time = System.currentTimeMillis();
            if (time >= redisTo.getStartTime() && time <= redisTo.getEndTime()) {
                // 1.????????????????????????id????????????
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();

                if (randomCode.equals(key) && killId.equals(skuId)) {
                    // 2.?????????????????? ????????????????????????id
                    BigDecimal limit = redisTo.getSeckillLimit();
                    //????????????????????????
                    if (num <= limit.intValue()) {
                        // 3.???????????????????????????????????????.???????????????????????????????????????????????????
                        String redisKey = memberRsepVo.getId() + "-" + skuId;
                        // ????????????????????? ????????????????????????
                        long ttl = redisTo.getEndTime() - redisTo.getStartTime();
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl < 0 ? 0 : ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            System.out.println(" ???????????? ?????????????????????");
                            // ???????????? ?????????????????????
                            //???????????????
                            RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + randomCode);
                            //tryAcquire ????????????????????? ?????????
                            boolean acquire = semaphore.tryAcquire(num);
                            if (acquire) {
                                // ????????????
                                // ???????????? ??????MQ
                                String orderSn = IdWorker.getTimeId() + UUID.randomUUID().toString().replace("-", "").substring(7, 8);
                                System.out.println("orderSn:" + orderSn);
                                SecKillOrderTo orderTo = new SecKillOrderTo();
                                orderTo.setOrderSn(orderSn);
                                orderTo.setMemberId(memberRsepVo.getId());
                                orderTo.setNum(num);
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return orderSn;
                            }
                        } else {
                            System.out.println(" ????????????");
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    @SentinelResource(value = "CurrentSeckillSkusResource", blockHandler = "blockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

        // 1.??????????????????????????????????????????
        long time = System.currentTimeMillis();
        // ??????????????????????????????
        try (Entry entry = SphU.entry("getCurrentSeckillSkus")) {
            Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            for (String key : keys) {
                // seckill:sessions:1593993600000_1593995400000
                String replace = key.replace("seckill:sessions:", "");
                String[] split = replace.split("_");
                long start = Long.parseLong(split[0]);
                long end = Long.parseLong(split[1]);
                if (time >= start && time <= end) {
                    // 2.?????????????????????????????????????????????
                    List<String> range = stringRedisTemplate.opsForList().range(key, 0, 100);
                    BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    List<String> list = hashOps.multiGet(range);
                    if (list != null) {
                        return list.stream().map(item -> {
                            SeckillSkuRedisTo redisTo = null;
                            try {
                                redisTo = new ObjectMapper().readValue(item, SeckillSkuRedisTo.class);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
//						redisTo.setRandomCode(null);
                            return redisTo;
                        }).collect(Collectors.toList());
                    }
                    break;
                }
            }
        } catch (BlockException e) {
            System.out.println("??????????????????getCurrentSeckillSkus????????????" + e.getMessage());
        }

        return null;
    }

    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {
        System.out.println("??????????????????CurrentSeckillSkusResource????????????" + e.getMessage());
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) throws JsonProcessingException {
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d-" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo to = new ObjectMapper().readValue(json, SeckillSkuRedisTo.class);
                    // ?????????????????????
                    long current = System.currentTimeMillis();

                    if (current <= to.getStartTime() || current >= to.getEndTime()) {
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }
        return null;
    }
}
