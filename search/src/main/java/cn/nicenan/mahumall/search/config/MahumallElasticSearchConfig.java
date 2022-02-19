package cn.nicenan.mahumall.search.config;


import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1.导入依赖
 * 2.配置类,给容器注入
 */
@Configuration
public class MahumallElasticSearchConfig {

    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.addHeader("Authorization", "Bearer " + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory
//                        .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

//    @Bean
//    public ElasticsearchClient esClient() {
//        // Create the low-level client
//        RestClient restClient = RestClient.builder(new HttpHost("192.168.2.211", 9200)).build();
//        // Create the transport with a Jackson mapper
//        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
//        // And create the API client
//        ElasticsearchClient client = new ElasticsearchClient(transport);
//        return client;
//    }

    @Bean
    public RestHighLevelClient esRestClient() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("192.168.2.211", 9200, "http")));
        return client;
    }
}
