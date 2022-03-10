package cn.nicenan.mahumall.order.vo;

import cn.nicenan.mahumall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

    /**
     * 错误状态码： 0----成功
     */
    private Integer code;
}
