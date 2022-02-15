package cn.nicenan.mahumall.coupon.service.impl;

import cn.nicenan.mahumall.common.to.MemberPrice;
import cn.nicenan.mahumall.common.to.SkuReductionTo;
import cn.nicenan.mahumall.coupon.entity.MemberPriceEntity;
import cn.nicenan.mahumall.coupon.entity.SkuLadderEntity;
import cn.nicenan.mahumall.coupon.service.MemberPriceService;
import cn.nicenan.mahumall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.coupon.dao.SkuFullReductionDao;
import cn.nicenan.mahumall.coupon.entity.SkuFullReductionEntity;
import cn.nicenan.mahumall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    private SkuLadderService skuLadderService;

    @Autowired
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo reductionTo) {
        //1.保存满减打折,会员价 sku的优惠满减信息 mahumall : sms-> sms_sku_ladder\sms_sku_full_reduction\sms_member_price
        //阶梯价格
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(reductionTo.getSkuId());
        skuLadderEntity.setFullCount(reductionTo.getFullCount());
        skuLadderEntity.setDiscount(reductionTo.getDiscount());
        skuLadderEntity.setAddOther(reductionTo.getCountStatus());
        if (reductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        //满减信息
        SkuFullReductionEntity skuFullReduction = new SkuFullReductionEntity();
        BeanUtils.copyProperties(reductionTo, skuFullReduction);
        if (skuFullReduction.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
            this.save(skuFullReduction);
        }

        //会员价格
        List<MemberPrice> memberPrice = reductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity priceEntity = new MemberPriceEntity();
            priceEntity.setSkuId(reductionTo.getSkuId());
            priceEntity.setMemberLevelId(item.getId());
            priceEntity.setMemberLevelName(item.getName());
            priceEntity.setMemberPrice(item.getPrice());
            priceEntity.setAddOther(1);
            return priceEntity;
        }).filter(item -> item.getMemberPrice().compareTo(new BigDecimal("0")) == 1).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);

    }

}
