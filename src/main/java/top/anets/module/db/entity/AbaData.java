package top.anets.module.db.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
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
 * @since 2024-07-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("aba_data")
@ApiModel(value="AbaData对象", description="")
public class AbaData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String searchTerms;

    private Integer searchFrequencyRank;

    private LocalDate dateRange;

    private String site;

    private String asinClicked1;

    private String asinClicked2;

    private String asinClicked3;

    private Double clickShare1;

    private Double clickShare2;

    private Double clickShare3;

    private Double transformShare1;

    private Double transformShare2;

    private Double transformShare3;

    private String brand1;

    private String brand2;

    private String brand3;

    private String color1;

    private String color2;

    private String color3;

    private String size1;

    private String size2;

    private String size3;

    private String cnWord;


}
