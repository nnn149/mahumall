package cn.nicenan.mahumall.ware.vo;

import lombok.Data;

@Data
public class SkuHasStockVo {
    private Long skuId;
    private Integer stock; // 库存数量
    private Boolean hasStock;
}
