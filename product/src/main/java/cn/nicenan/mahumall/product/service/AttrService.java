package cn.nicenan.mahumall.product.service;

import cn.nicenan.mahumall.product.vo.AttrGroupRelationVo;
import cn.nicenan.mahumall.product.vo.AttrRespVo;
import cn.nicenan.mahumall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:28
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId 分组id
     * @return
     */
    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleterRelation(AttrGroupRelationVo[] vos);

    /***
     * 获取当前分组没有关联的所有基本属性
     * @param params 分页
     * @param attrgroupId 属性分组id
     * @return
     */
    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);
}

