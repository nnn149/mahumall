package cn.nicenan.mahumall.product.dao;

import cn.nicenan.mahumall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:27
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
