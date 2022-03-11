package cn.nicenan.mahumall.product.service.impl;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.product.entity.SkuImagesEntity;
import cn.nicenan.mahumall.product.entity.SpuInfoDescEntity;
import cn.nicenan.mahumall.product.feign.SeckillFeignService;
import cn.nicenan.mahumall.product.service.AttrGroupService;
import cn.nicenan.mahumall.product.service.SkuSaleAttrValueService;
import cn.nicenan.mahumall.product.service.SpuInfoDescService;
import cn.nicenan.mahumall.product.vo.*;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.product.dao.SkuInfoDao;
import cn.nicenan.mahumall.product.entity.SkuInfoEntity;
import cn.nicenan.mahumall.product.service.SkuInfoService;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SkuImagesServiceImpl imagesService;
    @Autowired
    AttrGroupService attrGroupService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w -> w.eq("sku_id", key).or().like("sku_name", key));
        }
        // 三级id没选择不应该拼这个条件  没选应该查询所有
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            // gt : 大于;  ge: 大于等于
            wrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal("0")) == 1) {
                    // le: 小于等于
                    wrapper.le("price", max);
                }
            } catch (Exception e) {
                System.out.println("cn.nicenan.mahumall.product.service.impl.SkuInfoServiceImpl：前端传来非数字字符");
            }
        }
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;

    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1.sku基本信息
            SkuInfoEntity info = getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, threadPoolExecutor);

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            //2.sku图片信息
            List<SkuImagesEntity> imagesEntities = imagesService.getimagesBySkuId(skuId);
            skuItemVo.setImages(imagesEntities);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuSaleAttrFuture = infoFuture.thenAcceptAsync(info -> {
            //3.spu销售属性组合
            List<ItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(info.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, threadPoolExecutor);
        CompletableFuture<Void> spuInfoFuture = infoFuture.thenAcceptAsync(info -> {
            //4.获取spu的介绍
            SpuInfoDescEntity spuinfen = spuInfoDescService.getById(info.getSpuId());
            skuItemVo.setDesc(spuinfen);
        }, threadPoolExecutor);
        CompletableFuture<Void> spuItemAttrFuture = infoFuture.thenAcceptAsync(info -> {
            //5.获取spu的规格参数
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getattrGroupWithAttrsBySpuId(info.getSpuId(), info.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, threadPoolExecutor);

        //查询秒杀优惠
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R<SeckillInfoVo> r = seckillFeignService.getSkuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckillInfoVo data = r.getData(new TypeReference<>() {
                });
                skuItemVo.setSeckillInfoVo(data);
            }
        }, threadPoolExecutor);


        //等待所有任务完成
        CompletableFuture.allOf(imageFuture, spuSaleAttrFuture, spuInfoFuture, spuItemAttrFuture, seckillFuture).get();
        return skuItemVo;
    }

}
