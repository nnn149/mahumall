package cn.nicenan.mahumall.product.service.impl;

import cn.nicenan.mahumall.common.constant.ProductConstant;
import cn.nicenan.mahumall.common.to.SkuHasStockTo;
import cn.nicenan.mahumall.common.to.SkuReductionTo;
import cn.nicenan.mahumall.common.to.SpuBoundTo;
import cn.nicenan.mahumall.common.to.es.SkuEsModel;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.product.entity.*;
import cn.nicenan.mahumall.product.feign.CouponFeignService;
import cn.nicenan.mahumall.product.feign.SearchFeignService;
import cn.nicenan.mahumall.product.feign.WareFeignService;
import cn.nicenan.mahumall.product.service.*;
import cn.nicenan.mahumall.product.vo.*;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SpuImagesService spuImagesService;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private ProductAttrValueService attrValueService;
    @Autowired
    private SearchFeignService searchFeignService;
    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), new QueryWrapper<SpuInfoEntity>());

        return new PageUtils(page);
    }

    /**
     * TODO 高级部分完善失败和事务处理
     *
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1.保存Spu基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        //2.保存Spu的描述图片 pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        //3.保存Spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);

        //4.保存Spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity id = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(id.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(infoEntity.getId());
            return valueEntity;
        }).collect(Collectors.toList());
        attrValueService.saveProductAttr(collect);

        //5.保存spu的积分信息 mahumall : sms->sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }


        //6.保存当前spu对应的所有sku信息
        //6.1 sku的基本信息 pms_sku_info
        List<Skus> skus = vo.getSkus();
        if (skus != null || skus.size() > 0) {
            skus.forEach(item -> {
                String defalutImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defalutImg = image.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defalutImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity -> StringUtils.isNotEmpty(entity.getImgUrl())).collect(Collectors.toList());
                //6.2 sku的图片信息 pms_sku_images
                skuImagesService.saveBatch(imagesEntities);
                //6.3 sku的销售属性 pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
                //6.4 sku的优惠满减信息 mahumall : sms-> sms_sku_ladder\sms_sku_full_reduction\sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存sku优惠信息失败");
                    }
                }
            });
        }


    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        // 根据 spu管理带来的条件进行叠加模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w -> w.eq("id", key).or().like("spu_name", key));
        }

        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public void up(Long spuId) {

        //1.组装需要的数据

        //查询sku(spu)所有可以被检索的规格属性
        List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr -> attr.getAttrId()).collect(Collectors.toList());
        List<Long> searchAttrId = attrService.selectSearchAttrs(attrIds);
        HashSet idSet = new HashSet(searchAttrId);
        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(attr -> idSet.contains(attr.getAttrId())).map(attr -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(attr, attrs);
            return attrs;
        }).collect(Collectors.toList());


        //1.查询当前spuid对应的sku，品牌
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        //发送远程调用,库存系统系统查询是否有库存
        Map<Long, Boolean> stockMap = null;
        try {
            //方式服务访问不到而失败
            List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
            stockMap = wareFeignService.getSkusHasStock(skuIds).getData(new TypeReference<>() {
            }).stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, item -> item.getStock() > 0));
        } catch (Exception e) {
            log.error("查询库存服务异常:" + e);
        }
        //2.封装每个sku信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> collect = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            //查询是否有库存
            if (finalStockMap == null) {
                esModel.setHasStock(true);
            } else {
                esModel.setHasStock(finalStockMap.get(esModel.getSkuId()));
            }
            //TODO 2.热度评分. 默认 0
            esModel.setHotScore(0L);
            //品牌名称 分类
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            //设置属性列表
            esModel.setAttrs(attrsList);
            return esModel;
        }).collect(Collectors.toList());


        //TODO 数据发给es
        R r = searchFeignService.productStatusUp(collect);
        if (r.getCode() == 0) {
            //远程调用成功 改变spu状态
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.UP_SPU.getCode());
        } else {
//TODO 远程调用失败 重复调用？接口幂等性,重试机制


        }
    }


}




