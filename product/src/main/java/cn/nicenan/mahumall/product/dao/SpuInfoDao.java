package cn.nicenan.mahumall.product.dao;

import cn.nicenan.mahumall.common.constant.ProductConstant;
import cn.nicenan.mahumall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:27
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updateSpuStatus(@Param("spuId") Long spuId, @Param("status") int status);
}
