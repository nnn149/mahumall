package cn.nicenan.mahumall.order.Feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.order.vo.SkuStockVo;
import cn.nicenan.mahumall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>Title: WmsFeignService</p>
 * Description：
 * date：2020/7/1 11:58
 */
@FeignClient("mahumall-ware")
public interface WmsFeignService {

    @PostMapping("/ware/waresku/hasStock")
    R<List<SkuStockVo>> getSkuHasStock(@RequestBody List<Long> SkuIds);

    @GetMapping("/ware/wareinfo/fare")
    R getFare(@RequestParam("addrId") Long addrId);

    /**
     * 锁定库存
     */
    @PostMapping("/ware/waresku/lock/order")
    R orderLockStock(@RequestBody WareSkuLockVo vo);
}
