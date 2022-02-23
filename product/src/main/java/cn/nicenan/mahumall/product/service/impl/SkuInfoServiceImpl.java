package cn.nicenan.mahumall.product.service.impl;

import cn.nicenan.mahumall.product.entity.SkuImagesEntity;
import cn.nicenan.mahumall.product.entity.SpuInfoDescEntity;
import cn.nicenan.mahumall.product.service.AttrGroupService;
import cn.nicenan.mahumall.product.service.SkuSaleAttrValueService;
import cn.nicenan.mahumall.product.service.SpuInfoDescService;
import cn.nicenan.mahumall.product.vo.Images;
import cn.nicenan.mahumall.product.vo.ItemSaleAttrVo;
import cn.nicenan.mahumall.product.vo.SkuItemVo;
import cn.nicenan.mahumall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItemVo = new SkuItemVo();
        //1.sku基本信息
        SkuInfoEntity info = getById(skuId);
        skuItemVo.setInfo(info);
        Long catalogId = info.getCatalogId();
        Long spuId = info.getSpuId();
        //2.sku图片信息
        List<SkuImagesEntity> imagesEntities = imagesService.getimagesBySkuId(skuId);
        skuItemVo.setImages(imagesEntities);
        //3.spu销售属性组合
        List<ItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(spuId);
        skuItemVo.setSaleAttr(saleAttrVos);
        //4.获取spu的介绍
        SpuInfoDescEntity spuinfen = spuInfoDescService.getById(spuId);
        skuItemVo.setDesc(spuinfen);
        //5.获取spu的规格参数
        List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getattrGroupWithAttrsBySpuId(spuId, catalogId);
        skuItemVo.setGroupAttrs(attrGroupVos);
        return skuItemVo;
    }

}
