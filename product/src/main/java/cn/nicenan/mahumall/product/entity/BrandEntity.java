package cn.nicenan.mahumall.product.entity;

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
    @TableId
    private Long brandId;
    /**
     * 品牌名
     */
    @NotBlank(message = "品牌名不能为空")
    private String name;
    /**
     * 品牌logo地址
     */
    @URL(message = "必须是一个合法的url地址")
    @NotEmpty
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
    @Pattern(regexp = "/^[a-zA-Z]$/", message = "检索首字母必须是一个字母")
    @NotEmpty
    private String firstLetter;
    /**
     * 排序
     */
    @Min(value = 0,message = "排序必须大于等于0")
    @NotNull
    private Integer sort;

}
