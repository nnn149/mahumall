package cn.nicenan.mahumall.product.dao;

import cn.nicenan.mahumall.product.entity.SkuSaleAttrValueEntity;
import cn.nicenan.mahumall.product.vo.ItemSaleAttrVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:26
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<ItemSaleAttrVo> getSaleAttrsBySpuId(@Param("spuId") Long spuId);

    List<String> getSkuSaleAttrValuesAsStringList(@Param("skuId") Long skuId);
}
