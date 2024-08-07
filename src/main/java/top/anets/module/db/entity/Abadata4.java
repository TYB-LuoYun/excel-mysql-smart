package top.anets.module.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author ftm
 * @since 2024-07-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`abadata4`")
@ApiModel(value="Abadata4对象", description="`")
public class Abadata4 implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("`搜索词`")
    private String searchTerms;

    @TableField("`搜索频率排名`")
    private Integer searchFrequencyRank;

    @TableField("`日期范围`")
    private LocalDate dateRange;

    @TableField("`站点`")
    private String site;

    @TableField("`#1 已点击的 ASIN`")
    private String asinClicked1;

    @TableField("`#2 已点击的 ASIN`")
    private String asinClicked2;

    @TableField("`#3 已点击的 ASIN`")
    private String  asinClicked3;
    @TableField("`#1 点击共享`")
    private String clickShare1;

    @TableField("`#2 点击共享`")
    private String clickShare2;
    @TableField("`#3 点击共享`")
    private String clickShare3;

    @TableField("`#1 转化共享`")
    private String transformShare1;
    @TableField("`#2 转化共享`")
    private String transformShare2;
    @TableField("`#3 转化共享`")
    private String transformShare3;


    @TableField("`1品牌`")
    private String brand1;
    @TableField("`2品牌`")
    private String brand2;
    @TableField("`3品牌`")
    private String brand3;
    @TableField("`1颜色`")
    private String color1;
    @TableField("`2颜色`")
    private String color2;
    @TableField("`3颜色`")
    private String color3;
    @TableField("`1尺寸`")
    private String size1;
    @TableField("`2尺寸`")
    private String size2;
    @TableField("`3尺寸`")
    private String size3;


    @TableField("`cnWord`")
    private String cnWord;

    @TableId(value = "`id`", type = IdType.AUTO)
    private Long id;


}
