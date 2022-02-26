package cn.nicenan.mahumall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "mahumall.thread")
//配置属性已经加到component里面了，用的时候可以直接传入方法参数
@Component
@Data
public class ThreadPoolConfigProperties {
    private Integer corePoolSize;
    private Integer maxPoolSize;
    private Integer queueCapacity;
    private Long keepAliveSeconds;
}
