package cn.nicenan.mahumall.product.service;

import cn.nicenan.mahumall.product.entity.SpuInfoDescEntity;
import cn.nicenan.mahumall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:27
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);
    PageUtils queryPageByCondition(Map<String, Object> params);
    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);


}

