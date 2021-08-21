package cn.nicenan.mahumall.coupon.dao;

import cn.nicenan.mahumall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 21:50:24
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
