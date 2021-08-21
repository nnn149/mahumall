package cn.nicenan.mahumall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.CategoryBrandRelationEntity;

import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:27
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

