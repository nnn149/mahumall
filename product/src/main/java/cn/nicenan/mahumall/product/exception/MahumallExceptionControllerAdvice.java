package cn.nicenan.mahumall.product.exception;

import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import cn.nicenan.mahumall.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 *
 * @author 10418
 */
@Slf4j
@RestControllerAdvice(basePackages = "cn.nicenan.mahumall.product.controller")
public class MahumallExceptionControllerAdvice {

    /**
     * 数据校验异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题:{},异常类型:{}", e.getMessage(), e.getClass());
        BindingResult result = e.getBindingResult();

        Map<String, String> map = new HashMap<>();
        //获取校验的错误结果
        result.getFieldErrors().forEach(item -> {
            //错误消息和字段名
            String message = item.getDefaultMessage();
            String field = item.getField();
            map.put(field, message);
        });
        return R.useEnum(BizCodeEnume.VALID_EXCEPTION).put("data", map);
    }

    /**
     * 其他异常/
     *
     * @param throwable
     * @return
     */
    @ExceptionHandler(Throwable.class)
    public R handleException(Throwable throwable) {
        log.error("错误", throwable);
        return R.useEnum(BizCodeEnume.UNKNOW_EXCEPTION);
    }
}
