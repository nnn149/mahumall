package cn.nicenan.mahumall.gateway.config;

import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import cn.nicenan.mahumall.common.utils.R;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.management.monitor.Monitor;

@Configuration
public class MySentinelConfig {
    public MySentinelConfig() {
        GatewayCallbackManager.setBlockHandler((exchange, throwable) -> {
            String s = "";
            System.out.println("网关流控");
            try {
                s = new ObjectMapper().writeValueAsString(R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(s), String.class);
            return body;

        });

    }
}
