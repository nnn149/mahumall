package cn.nicenan.mahumall.auth.controller;

import cn.nicenan.mahumall.auth.feign.MemberFeignService;
import cn.nicenan.mahumall.common.constant.AuthServerConstant;
import cn.nicenan.mahumall.common.to.MemberRespTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Slf4j
@Controller
@RequestMapping("/oauth2.0")
public class Oath2Controller {


    @GetMapping("/logout")
    public String login(HttpSession session){
        if(session.getAttribute(AuthServerConstant.LOGIN_USER) != null){
            log.info("\n[" + ((MemberRespTo)session.getAttribute(AuthServerConstant.LOGIN_USER)).getUsername() + "] 已下线");
        }
        session.invalidate();
        return "redirect:http://auth.mahumall.com/login.html";
    }

}
