package cn.nicenan.mahumall.auth.feign;

import cn.nicenan.mahumall.auth.vo.SocialUser;
import cn.nicenan.mahumall.auth.vo.UserLoginVo;
import cn.nicenan.mahumall.auth.vo.UserRegisterVo;
import cn.nicenan.mahumall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mahumall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    R<String> register(@RequestBody UserRegisterVo userRegisterVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R login(@RequestBody SocialUser socialUser);
}

