package cn.nicenan.mahumall.product.entity;

import cn.nicenan.mahumall.common.valid.AddGroup;
import cn.nicenan.mahumall.common.valid.UpdateGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * JSR303 校验
 * 1. 给Bean添加校验注解,可以自定义message提示
 * 2. 参数加上 @Valid 注解开启校验功能
 * 3. 校验参数后紧跟一个BindingResult参数，即可拿到校验结果
 * 4. 统一异常处理 使用Spring MVC提供的 @RestControllerAdvice   方法上 @ExceptionHandler
 * 5. 分组校验 1.校验注解上加上groups属性传入一个接口类进行标注。2.controller的参数使用@Validated传入接口类。没有标注分组的校验不起作用,不分组时才生效
 */

/**
 * 品牌
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 16:53:28
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌id
     */
    @Null(message = "新增不能指定id", groups = {AddGroup.class})
    @NotNull(message = "修改必须指定品牌id", groups = {UpdateGroup.class})
    @TableId
    private Long brandId;
    /**
     * 品牌名
     */
    @NotBlank(message = "品牌名不能为空", groups = {AddGroup.class})
    private String name;
    /**
     * 品牌logo地址
     */
    @URL(message = "必须是一个合法的url地址", groups = {AddGroup.class, UpdateGroup.class})
    @NotBlank(groups = {AddGroup.class})
    private String logo;
    /**
     * 介绍
     */
    private String descript;
    /**
     * 显示状态[0-不显示；1-显示]
     */
    private Integer showStatus;
    /**
     * 检索首字母
     */
    @Pattern(regexp = "/^[a-zA-Z]$/", message = "检索首字母必须是一个字母", groups = {AddGroup.class, UpdateGroup.class})
    @NotBlank(groups = {AddGroup.class})
    private String firstLetter;
    /**
     * 排序
     */
    @Min(value = 0, message = "排序必须大于等于0", groups = {AddGroup.class, UpdateGroup.class})
    @NotNull(groups = {AddGroup.class})
    private Integer sort;

}
