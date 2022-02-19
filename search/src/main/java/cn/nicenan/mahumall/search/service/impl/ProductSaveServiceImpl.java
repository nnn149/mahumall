package cn.nicenan.mahumall.search.service.impl;

import cn.nicenan.mahumall.common.to.es.SkuEsModel;
import cn.nicenan.mahumall.search.config.MahumallElasticSearchConfig;
import cn.nicenan.mahumall.search.constant.EsConstant;
import cn.nicenan.mahumall.search.service.ProductSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //保存到es
        //1. 给es中建立索引，建立映射关系

        //2.es保存数据，批量保存
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel s : skuEsModels) {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(String.valueOf(s.getSkuId()));
            String json = new ObjectMapper().writeValueAsString(s);
            indexRequest.source(json, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, MahumallElasticSearchConfig.COMMON_OPTIONS);
        //TODO 如果出现错误，处理
        if (bulk.hasFailures()) {
            List<String> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
            log.error("商品商家错误:{}", collect);
            return false;
        }
        return true;
    }
}
