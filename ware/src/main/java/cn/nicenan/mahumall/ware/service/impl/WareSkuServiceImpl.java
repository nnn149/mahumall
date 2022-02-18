package cn.nicenan.mahumall.ware.service.impl;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.ware.feign.ProductFeignService;
import cn.nicenan.mahumall.ware.service.WareOrderTaskDetailService;
import cn.nicenan.mahumall.ware.service.WareOrderTaskService;
import cn.nicenan.mahumall.ware.vo.SkuHasStockVo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
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

import cn.nicenan.mahumall.ware.dao.WareSkuDao;
import cn.nicenan.mahumall.ware.entity.WareSkuEntity;
import cn.nicenan.mahumall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Resource
    private WareSkuDao wareSkuDao;

    @Resource
    private ProductFeignService productFeignService;

    @Autowired
    private WareOrderTaskService orderTaskService;

    @Autowired
    private WareOrderTaskDetailService orderTaskDetailService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String id = (String) params.get("skuId");
        if(!StringUtils.isEmpty(id)){
            wrapper.eq("sku_id", id);
        }
        id = (String) params.get("wareId");
        if(!StringUtils.isEmpty(id)){
            wrapper.eq("ware_id", id);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }


    /**
     * 添加库存
     * wareId: 仓库id
     * return 返回商品价格
     */
    @Transactional
    @Override
    public double addStock(Long skuId, Long wareId, Integer skuNum) {
        // 1.如果还没有这个库存记录 那就是新增操作
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        double price = 0.0;
        // TODO 还可以用什么办法让异常出现以后不回滚？高级
        WareSkuEntity entity = new WareSkuEntity();
        try {
            R info = productFeignService.info(skuId);
            Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");

            if(info.getCode() == 0){
                entity.setSkuName((String) data.get("skuName"));
                // 设置商品价格
                price = (Double) data.get("price");
            }
        }catch (Exception e){
            System.out.println("cn.firenay.mall.ware.service.impl.WareSkuServiceImpl：远程调用出错");
        }
        // 新增操作
        if(entities == null || entities.size() == 0){
            entity.setSkuId(skuId);
            entity.setStock(skuNum);
            entity.setWareId(wareId);
            entity.setStockLocked(0);
            wareSkuDao.insert(entity);
        }else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
        return price;
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = baseMapper.selectMaps(new QueryWrapper<WareSkuEntity>()
                .select("SUM(stock-stock_locked) ture_stock,sku_id").in("sku_id", skuIds)
                .groupBy("sku_id")).stream().map(map -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId((Long) map.get("sku_id"));
            vo.setStock(((BigDecimal) map.get("ture_stock")).intValue());
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

}
