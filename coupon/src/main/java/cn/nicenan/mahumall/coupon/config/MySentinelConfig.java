package cn.nicenan.mahumall.coupon.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class MySentinelConfig implements BlockExceptionHandler {
    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse response, BlockException e) throws Exception {

        // 不同的异常返回不同的提示语
        String message = null;
        if (e instanceof FlowException) {
            message = "被限流了";
        } else if (e instanceof DegradeException) {
            message = "服务降级了";
        } else if (e instanceof ParamFlowException) {
            message = "被限流了";
        } else if (e instanceof SystemBlockException) {
            message = "被系统保护了";
        } else if (e instanceof AuthorityException) {
            message = "被授权了";
        }
        response.setCharacterEncoding("utf-8");
        response.setHeader("content-Type", "application/json");
        response.getWriter().print("{\"status\":\"1\", \"message\":\"" + message + "\"}");
    }
}
