package cn.nicenan.mahumall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class ThirdPartyApplicationTests {

    //oss相关配置在nacos配置中心配置
    @Autowired
    private OSSClient ossClient;

    @Test
    void contextLoads() {
        ossClient.getObject(new GetObjectRequest("mahumall", "bootstrap.properties"), new File("./asd.txt"));
    }

}
