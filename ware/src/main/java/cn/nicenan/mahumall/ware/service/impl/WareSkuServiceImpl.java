package cn.nicenan.mahumall.ware.service.impl;

import cn.nicenan.mahumall.common.enume.OrderStatusEnum;
import cn.nicenan.mahumall.common.exception.NotStockException;
import cn.nicenan.mahumall.common.to.mq.OrderTo;
import cn.nicenan.mahumall.common.to.mq.StockDetailTo;
import cn.nicenan.mahumall.common.to.mq.StockLockedTo;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.ware.entity.WareOrderTaskDetailEntity;
import cn.nicenan.mahumall.ware.entity.WareOrderTaskEntity;
import cn.nicenan.mahumall.ware.feign.OrderFeignService;
import cn.nicenan.mahumall.ware.feign.ProductFeignService;
import cn.nicenan.mahumall.ware.service.WareOrderTaskDetailService;
import cn.nicenan.mahumall.ware.service.WareOrderTaskService;
import cn.nicenan.mahumall.ware.vo.OrderItemVo;
import cn.nicenan.mahumall.ware.vo.OrderVo;
import cn.nicenan.mahumall.ware.vo.SkuHasStockVo;
import cn.nicenan.mahumall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.ware.dao.WareSkuDao;
import cn.nicenan.mahumall.ware.entity.WareSkuEntity;
import cn.nicenan.mahumall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Resource
    private WareSkuDao wareSkuDao;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Resource
    private ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskService orderTaskService;
    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;
    @Value("${myRabbitmq.MQConfig.eventExchange}")
    private String eventExchange;

    @Value("${myRabbitmq.MQConfig.routingKey}")
    private String routingKey;
    @Autowired
    private OrderFeignService orderFeignService;


    @Override
    public void unlockStock(StockLockedTo to) {
        log.info("\n收到解锁库存的消息");
        // 库存id
        Long id = to.getId();
        StockDetailTo detailTo = to.getDetailTo();
        Long detailId = detailTo.getId();
        /**
         * 解锁库存
         * 	查询数据库关系这个订单的详情
         * 		有: 证明库存锁定成功
         * 			1.没有这个订单, 必须解锁
         * 			2.有这个订单 不是解锁库存
         * 				订单状态：已取消,解锁库存
         * 				没取消：不能解锁	;
         * 		没有：就是库存锁定失败， 库存回滚了 这种情况无需回滚
         */
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null) {
            // 解锁
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            // 根据订单号 查询订单状态 已取消才解锁库存
            R<OrderVo> orderStatus = orderFeignService.getOrderStatus(orderSn);
            if (orderStatus.getCode() == 0) {
                // 订单数据返回成功
                OrderVo orderVo = orderStatus.getData(new TypeReference<>() {
                });
                // 订单不存在
                if (orderVo == null || orderVo.getStatus().equals(OrderStatusEnum.CANCLED.getCode())) {
                    // 订单已取消 状态1 已锁定  这样才可以解锁
                    if (byId.getLockStatus() == 1) {
                        unLock(detailTo.getSkuId(), detailTo.getWareId(), detailTo.getSkuNum(), detailId);
                    }
                }
            } else {
                // 消息拒绝 重新放回队列 让别人继续消费解锁
                throw new RuntimeException("远程服务失败");
            }
        } else {
            // 无需解锁
        }
    }


    /**
     * 解锁库存
     */
    private void unLock(Long skuId, Long wareId, Integer num, Long taskDeailId) {
        // 更新库存
        wareSkuDao.unlockStock(skuId, wareId, num);
        // 更新库存工作单的状态
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(taskDeailId);
        detailEntity.setLockStatus(2);
        orderTaskDetailService.updateById(detailEntity);
    }

    /**
     * 防止订单服务卡顿 导致订单状态一直改不了 库存消息有限到期 最后导致卡顿的订单 永远无法解锁库存
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo to) {
        log.info("\n订单超时自动关闭,准备解锁库存");
        String orderSn = to.getOrderSn();
        // 查一下最新的库存状态 防止重复解锁库存[Order服务可能会提前解锁]
        WareOrderTaskEntity taskEntity = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long taskEntityId = taskEntity.getId();
        // 按照工作单找到所有 没有解锁的库存 进行解锁 状态为1等于已锁定
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", taskEntityId).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String id = (String) params.get("skuId");
        if (!StringUtils.isEmpty(id)) {
            wrapper.eq("sku_id", id);
        }
        id = (String) params.get("wareId");
        if (!StringUtils.isEmpty(id)) {
            wrapper.eq("ware_id", id);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }


    /**
     * 添加库存
     * wareId: 仓库id
     * return 返回商品价格
     */
    @Transactional
    @Override
    public double addStock(Long skuId, Long wareId, Integer skuNum) {
        // 1.如果还没有这个库存记录 那就是新增操作
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        double price = 0.0;
        // TODO 还可以用什么办法让异常出现以后不回滚？高级
        WareSkuEntity entity = new WareSkuEntity();
        try {
            R info = productFeignService.info(skuId);
            Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");

            if (info.getCode() == 0) {
                entity.setSkuName((String) data.get("skuName"));
                // 设置商品价格
                price = (Double) data.get("price");
            }
        } catch (Exception e) {
            System.out.println("cn.nicenan.mall.ware.service.impl.WareSkuServiceImpl：远程调用出错");
        }
        // 新增操作
        if (entities == null || entities.size() == 0) {
            entity.setSkuId(skuId);
            entity.setStock(skuNum);
            entity.setWareId(wareId);
            entity.setStockLocked(0);
            wareSkuDao.insert(entity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
        return price;
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = baseMapper.selectMaps(new QueryWrapper<WareSkuEntity>()
                .select("SUM(stock-stock_locked) ture_stock,sku_id").in("sku_id", skuIds)
                .groupBy("sku_id")).stream().map(map -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId((Long) map.get("sku_id"));
            vo.setStock(((BigDecimal) map.get("ture_stock")).intValue());
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        return skuIds.stream().map(id -> {
            SkuHasStockVo stockVo = new SkuHasStockVo();

            // 查询当前sku的总库存量
            stockVo.setSkuId(id);
            // 这里库存可能为null 要避免空指针异常
            Long skuStock = baseMapper.getSkuStock(id);
            stockVo.setHasStock(skuStock > 0);
            stockVo.setStock(Math.toIntExact(skuStock));
            return stockVo;
        }).collect(Collectors.toList());
    }

    /**
     * 为某个订单锁定库存
     *
     * @param vo
     */
    //默认运行时异常都回滚
    @Transactional(rollbackFor = NotStockException.class)
    @Override
    public void orderLockStock(WareSkuLockVo vo) {
        //保存库存工作单的详情。追溯
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);

        //1.找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                throw new NotStockException("商品" + skuId + "没有库存");
            }
            //1.如果每一个商品都锁定成功，将当前商品锁定了几年的工作单记录发给MQ
            //2.锁定失败，前面保存的工作单信息回滚，发送出去的消息，即使要解锁 但是没有id 无需解锁
            //3
            for (Long wareId : wareIds) {
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count != null || count == 1) {
                    //当前仓库锁失败,重试下一个仓库
                    skuStocked = true;
                    //TODO 发送库存锁定成功
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(taskDetailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                } else {
                    skuStocked = false;
                }
            }
            if (!skuStocked) {
                //当前商品所有仓库都没有锁住
                throw new NotStockException("商品" + skuId + "没有库存");
            }
        }
        //3肯定全部都是锁定成功的
        return;
    }

    @Data
    class SkuWareHasStock {

        private Long skuId;

        private List<Long> wareId;

        private Integer num;
    }

}
