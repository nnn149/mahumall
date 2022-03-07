package cn.nicenan.mahumall.ware.feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.ware.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mahumall-member")
public interface MemberFeignService {

    @RequestMapping("/member/memberreceiveaddress/info/{id}")
    R<MemberAddressVo> addrInfo(@PathVariable("id") Long id);
}
