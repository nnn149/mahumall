package cn.nicenan.mahumall.search.feign;

import cn.nicenan.mahumall.search.vo.AttrResponseVo;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.search.vo.BrandVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient("mahumall-product")
public interface ProductFeignService {
    @GetMapping("/product/attr/info/{attrId}")
    R<AttrResponseVo> getAttrsInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand/infos")
    R<List<BrandVo>> brandInfo(@RequestParam("brandIds") List<Long> brandIds);
}
