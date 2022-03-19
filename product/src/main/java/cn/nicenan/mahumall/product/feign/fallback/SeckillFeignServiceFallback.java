package cn.nicenan.mahumall.product.feign.fallback;

import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.product.feign.SeckillFeignService;
import cn.nicenan.mahumall.product.vo.SeckillInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R<SeckillInfoVo> getSkuSeckillInfo(Long skuId) {
        log.error("调用秒杀服务异常getSkuSeckillInfo");
        return R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg());
    }
}
