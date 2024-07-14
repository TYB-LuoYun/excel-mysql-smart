package top.anets.module.excel.vo;
import top.anets.module.excel.entity.ExcelMysql;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class  ExcelMysqlVo extends ExcelMysql{
   LinkedHashMap<String,String> fieldMap;
}