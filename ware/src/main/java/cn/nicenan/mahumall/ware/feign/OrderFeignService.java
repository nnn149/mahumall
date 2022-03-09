package cn.nicenan.mahumall.ware.feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.ware.vo.OrderVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mahumall-order")
public interface OrderFeignService {

    /**
     * 查询订单状态
     */
    @GetMapping("/order/order/status/{orderSn}")
    R<OrderVo> getOrderStatus(@PathVariable("orderSn") String orderSn);
}
