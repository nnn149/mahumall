package cn.nicenan.mahumall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1.开启服务注册发现 配置nacos注册中心地址
 */
@EnableDiscoveryClient
//排除和数据源的相关配置，应为common组件有mybatis-plus，但是gateway没用到。也可以在pom.xml的common依赖中排除mybatis-plys
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
