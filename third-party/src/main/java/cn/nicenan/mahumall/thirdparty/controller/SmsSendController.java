package cn.nicenan.mahumall.thirdparty.controller;

import cn.nicenan.mahumall.common.utils.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @GetMapping("/sendcode")
    public R sendCode(@RequestParam String phone, @RequestParam String code) {
        System.out.println("发送验证码：" + code + "到" + phone);
        return R.ok();
    }
}

