package cn.nicenan.mahumall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.ware.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 22:44:05
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

