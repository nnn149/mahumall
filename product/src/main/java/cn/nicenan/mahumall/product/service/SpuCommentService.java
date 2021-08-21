package cn.nicenan.mahumall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.SpuCommentEntity;

import java.util.Map;

/**
 * 商品评价
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:26
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

