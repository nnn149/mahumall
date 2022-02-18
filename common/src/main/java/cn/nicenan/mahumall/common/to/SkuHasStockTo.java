package cn.nicenan.mahumall.common.to;

import lombok.Data;

@Data
public class SkuHasStockTo {
    private Long skuId;
    private Integer stock; // 库存数量
}
