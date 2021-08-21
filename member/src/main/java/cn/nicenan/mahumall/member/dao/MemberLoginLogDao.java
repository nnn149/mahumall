package cn.nicenan.mahumall.member.dao;

import cn.nicenan.mahumall.member.entity.MemberLoginLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员登录记录
 * 
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:32:49
 */
@Mapper
public interface MemberLoginLogDao extends BaseMapper<MemberLoginLogEntity> {
	
}
