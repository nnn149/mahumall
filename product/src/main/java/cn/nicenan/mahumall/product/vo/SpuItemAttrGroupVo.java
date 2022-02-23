package cn.nicenan.mahumall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class SpuItemAttrGroupVo {
    private String groupName;

    private List<SpuBaseAttrVo> attrs;
}
