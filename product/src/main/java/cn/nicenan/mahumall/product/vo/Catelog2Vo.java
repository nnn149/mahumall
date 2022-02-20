package cn.nicenan.mahumall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Catelog2Vo {
    private String id;

    private String name;

    private String catalog1Id;

    private List<Catalog3Vo> catalog3List;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Catalog3Vo {

        private String id;

        private String name;

        private String catalog2Id;
    }
}
