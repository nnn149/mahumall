package cn.nicenan.mahumall.product.vo;

import cn.nicenan.mahumall.product.entity.SkuImagesEntity;
import cn.nicenan.mahumall.product.entity.SkuInfoEntity;
import cn.nicenan.mahumall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * <p>Title: SkuItemVo</p>
 * Description：
 * date：2020/6/24 13:33
 */
@Data
public class SkuItemVo {

    /**
     * 基本信息
     */
    SkuInfoEntity info;

    boolean hasStock = true;

    /**
     * 图片信息
     */
    List<SkuImagesEntity> images;

    /**
     * 销售属性组合
     */
    List<ItemSaleAttrVo> saleAttr;

    /**
     * spu介绍
     */
    SpuInfoDescEntity desc;

    /**
     * 参数规格信息
     */
    List<SpuItemAttrGroupVo> groupAttrs;
    SeckillInfoVo seckillInfoVo;

}
