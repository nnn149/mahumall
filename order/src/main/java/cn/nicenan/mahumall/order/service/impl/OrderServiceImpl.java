package cn.nicenan.mahumall.order.service.impl;

import cn.nicenan.mahumall.common.enume.OrderStatusEnum;
import cn.nicenan.mahumall.common.exception.NotStockException;
import cn.nicenan.mahumall.common.to.MemberRespTo;
import cn.nicenan.mahumall.common.to.mq.OrderTo;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.order.Feign.CartFeignService;
import cn.nicenan.mahumall.order.Feign.MemberFeignService;
import cn.nicenan.mahumall.order.Feign.ProductFeignService;
import cn.nicenan.mahumall.order.Feign.WmsFeignService;
import cn.nicenan.mahumall.order.to.OrderCreateTo;
import cn.nicenan.mahumall.order.constant.OrderConstant;
import cn.nicenan.mahumall.order.entity.OrderItemEntity;
import cn.nicenan.mahumall.order.interceptor.LoginUserInterceptor;
import cn.nicenan.mahumall.order.service.OrderItemService;
import cn.nicenan.mahumall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.order.dao.OrderDao;
import cn.nicenan.mahumall.order.entity.OrderEntity;
import cn.nicenan.mahumall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    private MemberFeignService memberFeignService;
    @Autowired
    private CartFeignService cartFeignService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private WmsFeignService wmsFeignService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${myRabbitmq.MQConfig.eventExchange}")
    private String eventExchange;

    @Value("${myRabbitmq.MQConfig.createOrder}")
    private String createOrder;

    @Value("${myRabbitmq.MQConfig.ReleaseOtherKey}")
    private String ReleaseOtherKey;
    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberRespTo memberRsepVo = LoginUserInterceptor.threadLocal.get();
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 这一步至关重要 冲主线程获取用户数据 异步线程来共享
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 异步线程共享 RequestContextHolder.getRequestAttributes()
            RequestContextHolder.setRequestAttributes(attributes);
            // 1.远程查询所有的收获地址列表
            List<MemberAddressVo> address;
            try {
                address = memberFeignService.getAddress(memberRsepVo.getId());
                confirmVo.setAddress(address);
            } catch (Exception e) {
                log.warn("\n远程调用会员服务失败 [会员服务可能未启动]");
            }
        }, executor);


        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            // 异步线程共享 RequestContextHolder.getRequestAttributes()
            RequestContextHolder.setRequestAttributes(attributes);
            // 2. 远程查询购物车服务
            // feign在远程调用之前要构造请求 调用很多拦截器
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            List<OrderItemVo> items = confirmVo.getItems();
            // 获取所有商品的id
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R<List<SkuStockVo>> hasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<>() {
            });
            if (data != null) {
                // 各个商品id 与 他们库存状态的映射
                Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(stocks);
            }
        }, executor);
        // 3.查询用户积分
        Integer integration = memberRsepVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 4.其他数据在类内部自动计算
        // TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRsepVo.getId(), token, 10, TimeUnit.MINUTES);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        // 当条线程共享这个对象
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo submitVo = new SubmitOrderResponseVo();
        // 0：正常
        submitVo.setCode(0);
        // 去服务器创建订单,验令牌,验价格,所库存
        MemberRespTo memberRsepVo = LoginUserInterceptor.threadLocal.get();
        // 1. 验证令牌 [必须保证原子性] 返回 0 or 1
        // 0 令牌删除失败 1删除成功
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();

        // 原子验证令牌 删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRsepVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌验证失败
            submitVo.setCode(1);
        } else {
            // 令牌验证成功
            // 1 .创建订单等信息
            OrderCreateTo order = createOrder();
            // 2. 验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal voPayPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(voPayPrice).doubleValue()) < 0.01) {
                // 金额对比成功
                // 3.保存订单
                saveOrder(order);
                // 4.库存锁定
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    // 锁定的skuId 这个skuId要锁定的数量
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());

                lockVo.setLocks(locks);
                // 远程锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    // 库存足够 锁定成功 给MQ发送消息
                    submitVo.setOrderEntity(order.getOrder());
                    rabbitTemplate.convertAndSend(this.eventExchange, this.createOrder, order.getOrder());
//                    int i = 10 / 0;
                } else {
                    // 锁定失败
                    String msg = (String) r.get("msg");
                    throw new NotStockException(msg);
                }
            } else {
                // 价格验证失败
                submitVo.setCode(2);
            }
        }
        return submitVo;
    }

    @Override

    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespTo rsepVo = LoginUserInterceptor.threadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                // 查询这个用户的最新订单 [降序排序]
                new QueryWrapper<OrderEntity>().eq("member_id", rsepVo.getId()).orderByDesc("id")
        );
        List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
            // 查询这个订单关联的所有订单项
            List<OrderItemEntity> orderSn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(orderSn);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(order_sn);
        return new PageUtils(page);
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        log.info("\n收到过期的订单信息(未支付)--准关闭订单:" + entity.getOrderSn());
        // 查询这个订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if(Objects.equals(orderEntity.getStatus(), OrderStatusEnum.CREATE_NEW.getCode())){
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            // 发送给MQ告诉它有一个订单被自动关闭了
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                // 保证消息 100% 发出去 每一个消息在数据库保存详细信息
                // 定期扫描数据库 将失败的消息在发送一遍
                rabbitTemplate.convertAndSend(eventExchange, ReleaseOtherKey , orderTo);
            } catch (AmqpException e) {
                // TODO 将没发送成功的消息进行重试发送.
            }
        }
    }

    /**
     * 创建订单
     */
    private OrderCreateTo createOrder() {

        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 1. 生成一个订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrderSn(orderSn);

        // 2. 获取所有订单项
        List<OrderItemEntity> items = buildOrderItems(orderSn);

        // 3.验价	传入订单 、订单项 计算价格、积分、成长值等相关信息
        computerPrice(orderEntity, items);
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(items);
        return orderCreateTo;
    }

    /**
     * 保存订单所有数据
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItems = orderItems.stream().map(item -> {
            item.setOrderId(orderEntity.getId());
            item.setSpuName(item.getSpuName());
            item.setOrderSn(order.getOrder().getOrderSn());
            return item;
        }).collect(Collectors.toList());
        orderItemService.saveBatch(orderItems);
    }

    private void computerPrice(OrderEntity orderEntity, List<OrderItemEntity> items) {
        BigDecimal totalPrice = new BigDecimal("0.0");
        // 叠加每一个订单项的金额
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity item : items) {
            // 优惠券的金额
            coupon = coupon.add(item.getCouponAmount());
            // 积分优惠的金额
            integration = integration.add(item.getIntegrationAmount());
            // 打折的金额
            promotion = promotion.add(item.getPromotionAmount());
            BigDecimal realAmount = item.getRealAmount();
            totalPrice = totalPrice.add(realAmount);

            // 购物获取的积分、成长值
            gift.add(new BigDecimal(item.getGiftIntegration().toString()));
            growth.add(new BigDecimal(item.getGiftGrowth().toString()));
        }
        // 1.订单价格相关 总额、应付总额
        orderEntity.setTotalAmount(totalPrice);
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);

        // 设置积分、成长值
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        // 设置订单的删除状态
        orderEntity.setDeleteStatus(OrderStatusEnum.CREATE_NEW.getCode());
    }

    /**
     * 为 orderSn 订单构建订单数据
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 这里是最后一次来确认购物项的价格 这个远程方法还会查询一次数据库
        List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCartItems();
        List<OrderItemEntity> itemEntities = null;
        if (cartItems != null && cartItems.size() > 0) {
            itemEntities = cartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
        }
        return itemEntities;
    }

    /**
     * 构建某一个订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1.订单信息： 订单号

        // 2.商品spu信息
        Long skuId = cartItem.getSkuId();
        R<SpuInfoVo> r = productFeignService.getSkuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData(new TypeReference<>() {
        });
        itemEntity.setSpuId(spuInfo.getId());
        itemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        itemEntity.setSpuName(spuInfo.getSpuName());
        itemEntity.setCategoryId(spuInfo.getCatalogId());
        // 3.商品的sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 把一个集合按照指定的字符串进行分割得到一个字符串
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        // 4.积分信息 买的数量越多积分越多 成长值越多
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        // 5.订单项的价格信息 优惠金额
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        // 当前订单项的实际金额
        BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        // 减去各种优惠的价格
        BigDecimal subtract = orign.subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);
        return itemEntity;
    }

    /**
     * 构建一个订单
     */
    private OrderEntity buildOrderSn(String orderSn) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setCreateTime(new Date());
        entity.setCommentTime(new Date());
        entity.setReceiveTime(new Date());
        entity.setDeliveryTime(new Date());
        MemberRespTo rsepVo = LoginUserInterceptor.threadLocal.get();
        entity.setMemberId(rsepVo.getId());
        entity.setMemberUsername(rsepVo.getUsername());
        entity.setBillReceiverEmail(rsepVo.getEmail());
        // 2. 获取收获地址信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R<FareVo> fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo resp = fare.getData(new TypeReference<>() {
        });
        entity.setFreightAmount(resp.getFare());
        entity.setReceiverCity(resp.getMemberAddressVo().getCity());
        entity.setReceiverDetailAddress(resp.getMemberAddressVo().getDetailAddress());
        entity.setDeleteStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setReceiverPhone(resp.getMemberAddressVo().getPhone());
        entity.setReceiverName(resp.getMemberAddressVo().getName());
        entity.setReceiverPostCode(resp.getMemberAddressVo().getPostCode());
        entity.setReceiverProvince(resp.getMemberAddressVo().getProvince());
        entity.setReceiverRegion(resp.getMemberAddressVo().getRegion());
        // 设置订单状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        return entity;
    }
}
