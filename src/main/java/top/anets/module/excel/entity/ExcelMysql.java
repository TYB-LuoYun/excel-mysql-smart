package top.anets.module.excel.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
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
 * @since 2024-07-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("excel_mysql")
@ApiModel(value="ExcelMysql对象", description="")
public class ExcelMysql implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "表名")
    private String tableName;

    @ApiModelProperty(value = "表格名")
    private String excelName;

    @ApiModelProperty(value = "表格行数")
    private Integer excelHeadRowNum;


    Integer dataStartRowNum;

    private String sheetName;

    private Integer sheetIndex;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer deleteFlag;


    private String updateBy;

    private String createBy;



}
