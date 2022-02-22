package cn.nicenan.mahumall.search.feign;

import cn.nicenan.mahumall.common.to.SkuHasStockTo;
import cn.nicenan.mahumall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mahumall-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasstock")
    R<List<SkuHasStockTo>> getSkusHasStock(@RequestBody List<Long> skuIds);
}
