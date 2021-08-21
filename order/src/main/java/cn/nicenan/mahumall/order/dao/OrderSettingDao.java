package cn.nicenan.mahumall.order.dao;

import cn.nicenan.mahumall.order.entity.OrderSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单配置信息
 * 
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:40:20
 */
@Mapper
public interface OrderSettingDao extends BaseMapper<OrderSettingEntity> {
	
}
