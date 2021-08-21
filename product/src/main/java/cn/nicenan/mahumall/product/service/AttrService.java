package cn.nicenan.mahumall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.AttrEntity;

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
}

