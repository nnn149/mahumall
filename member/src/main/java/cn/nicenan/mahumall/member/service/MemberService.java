package cn.nicenan.mahumall.member.service;

import cn.nicenan.mahumall.member.exception.PhoneExistException;
import cn.nicenan.mahumall.member.exception.UserNameExistException;
import cn.nicenan.mahumall.member.vo.UserRegisterVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:32:49
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(UserRegisterVo userRegisterVo);

    void checkPhone(String phone) throws PhoneExistException;

    void checkUserName(String username) throws UserNameExistException;
}

