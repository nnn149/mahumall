package cn.nicenan.mahumall.ware.service;

import cn.nicenan.mahumall.common.to.mq.OrderTo;
import cn.nicenan.mahumall.common.to.mq.StockLockedTo;
import cn.nicenan.mahumall.ware.vo.SkuHasStockVo;
import cn.nicenan.mahumall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:44:05
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    double addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    void orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    /**
     * 由于订单超时而自动释放订单之后来解锁库存
     */
    void unlockStock(OrderTo to);
}

