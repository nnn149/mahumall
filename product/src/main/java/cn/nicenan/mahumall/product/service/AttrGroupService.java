package cn.nicenan.mahumall.product.service;

import cn.nicenan.mahumall.product.vo.AttrGroupWithAttrsVo;
import cn.nicenan.mahumall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:28
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long categoryId);

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     * @param catelogId
     * @return
     */
    List<AttrGroupWithAttrsVo> getattrGroupWithAttrsByCatelogId(Long catelogId);

    List<SpuItemAttrGroupVo> getattrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

