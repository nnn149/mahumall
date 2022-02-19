package cn.nicenan.mahumall.search.controller;

import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import cn.nicenan.mahumall.common.to.es.SkuEsModel;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/search/save")
@RestController
@Slf4j
public class ElasticSaveController {
    @Autowired
    public ProductSaveService productSaveService;


    /**
     * 上架商品
     *
     * @param skuEsModels
     * @return
     */
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
        boolean r = false;
        try {
            r = productSaveService.productStatusUp(skuEsModels);
        } catch (Exception e) {
            log.error("商品上架失败，{}", e);
            return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if (r) {
            return R.ok();
        }
        return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
