package cn.nicenan.mahumall.ware.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class WareSkuServiceImplTest {

    @Autowired
    private WareSkuServiceImpl wareSkuService;
    @Test
    void getSkusHasStock() {
        ArrayList<Long> ids = new ArrayList<>();
        Collections.addAll(ids, 1L, 8L, 9L, 10L, 11L);
        wareSkuService.getSkusHasStock(ids);
    }
}
