package cn.nicenan.mahumall.ware.service.impl;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.ware.feign.MemberFeignService;
import cn.nicenan.mahumall.ware.vo.FareVo;
import cn.nicenan.mahumall.ware.vo.MemberAddressVo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.Query;

import cn.nicenan.mahumall.ware.dao.WareInfoDao;
import cn.nicenan.mahumall.ware.entity.WareInfoEntity;
import cn.nicenan.mahumall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq("id", key).or().like("name", key).or().like("address", key).or().like("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {

        R<MemberAddressVo> info = memberFeignService.addrInfo(addrId);
        FareVo fareVo = new FareVo();
        MemberAddressVo addressVo = info.getData(new TypeReference<>() {
        });
        fareVo.setMemberAddressVo(addressVo);
        if (addressVo != null) {
            String phone = addressVo.getPhone();
            if (phone == null || phone.length() < 2) {
                phone = new Random().nextInt(100) + "";
            }
            BigDecimal decimal = new BigDecimal(phone.substring(phone.length() - 1));
            fareVo.setFare(decimal);
        } else {
            fareVo.setFare(new BigDecimal("20"));
        }
        return fareVo;
    }

}
