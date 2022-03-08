package cn.nicenan.mahumall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import cn.nicenan.mahumall.common.exception.NotStockException;
import cn.nicenan.mahumall.ware.vo.SkuHasStockVo;
import cn.nicenan.mahumall.ware.vo.WareSkuLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.nicenan.mahumall.ware.entity.WareSkuEntity;
import cn.nicenan.mahumall.ware.service.WareSkuService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.R;


/**
 * 商品库存
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:44:05
 */
@RestController
@Slf4j
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo) {
        try {
            wareSkuService.orderLockStock(vo);
            return R.ok();
        } catch (NotStockException e) {
            log.warn("\n" + e.getMessage());
        }
        return R.error(BizCodeEnume.NOT_STOCK_EXCEPTION.getCode(), BizCodeEnume.NOT_STOCK_EXCEPTION.getMsg());
    }

    /**
     * 查询sku是否有库存
     * 返回当前id stock量
     */
    @PostMapping("/hasStock")
//	public List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> SkuIds){
    public R getSkuHasStock(@RequestBody List<Long> SkuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(SkuIds);
        return R.ok().setData(vos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }

    //查询sku是否有库存
    @PostMapping("/hasstock")
    public R<List<SkuHasStockVo>> getSkusHasStock(@RequestBody List<Long> skuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getSkusHasStock(skuIds);
        return new R<List<SkuHasStockVo>>().setData(vos);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
