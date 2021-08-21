package cn.nicenan.mahumall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.product.entity.SpuImagesEntity;

import java.util.Map;

/**
 * spu图片
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:26
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

