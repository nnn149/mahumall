package cn.nicenan.mahumall.auth.controller;

import cn.nicenan.mahumall.auth.feign.ThirdPartFeignService;
import cn.nicenan.mahumall.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class LoginController {
    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //验证码存redis
        String code = UUID.randomUUID().toString().substring(0, 4);
        thirdPartFeignService.sendCode(phone, code);
        return R.ok();
    }
}
