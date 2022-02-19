package cn.nicenan.mahumall.search;

import cn.nicenan.mahumall.search.config.MahumallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class SearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;


    @Test
    void contextLoads() {
        System.out.println(client);
    }

    /**
     * 测试保存数据
     */
    @Test
    void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.id("1");
//        indexRequest.source("userName","张三","age","18","gender","男");
//        User user=new User();
//        String userjson = "";
//        indexRequest.source(userjson, XContentType.JSON);
        IndexResponse index = client.index(indexRequest, MahumallElasticSearchConfig.COMMON_OPTIONS);


    }

    @Test
    void searchData() throws IOException {
        //1.创建检索亲求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");

        //指定DSL，检索条件，构建查询语句用
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //1.1构造检索条件
//        searchSourceBuilder.query();  根操作是query
//        searchSourceBuilder.from();  分页开始
//        searchSourceBuilder.size();   分页大小
//        searchSourceBuilder.aggregation();   聚合
//        searchSourceBuilder.sort();   排序
//        searchSourceBuilder.highlight();   高亮

//        QueryBuilders // xxxBuilders是xxx的工具类
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        //年龄分布聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);
        //平均工资
        searchSourceBuilder.aggregation(AggregationBuilders.avg("avgSalary").field("salary"));
        System.out.println(searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);

        //2.执行检索
        SearchResponse searchResponse = client.search(searchRequest, MahumallElasticSearchConfig.COMMON_OPTIONS);
        //3.分析结果
        System.out.println(searchResponse.toString());

        //3.1获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            hit.getIndex();
            hit.getId();
            hit.getSourceAsString();//json
        }
        //3.2拿到所有的聚合信息
        Aggregations aggregations = searchResponse.getAggregations();
        for (Aggregation aggregation : aggregations) {
            aggregation.getName();
            aggregation.getType();
        }
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            bucket.getKeyAsString();//年龄
            bucket.getDocCount();//几个人
        }
        Avg avgSalary = aggregations.get("avgSalary");//平均工资
        avgSalary.getValue();//平均值
    }

    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }
}
