package cn.nicenan.mahumall.order.interceptor;


import cn.nicenan.mahumall.common.constant.AuthServerConstant;
import cn.nicenan.mahumall.common.to.MemberRespTo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>Title: LoginUserInterceptor</p>
 * Description：
 * date：2020/6/29 22:40
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        // 这个请求直接放行
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        if(match){
            return true;
        }
        HttpSession session = request.getSession();
        MemberRespTo memberRsepVo = (MemberRespTo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(memberRsepVo != null){
            threadLocal.set(memberRsepVo);
            return true;
        }else{
            // 没登陆就去登录
            session.setAttribute("msg", AuthServerConstant.NOT_LOGIN);
            response.sendRedirect("http://auth.mahumall.com/login.html");
            return false;
        }
    }
}
