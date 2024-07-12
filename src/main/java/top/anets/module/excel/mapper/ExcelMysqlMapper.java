package top.anets.module.excel.mapper;

import top.anets.module.excel.entity.ExcelMysql;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
    * mapper接口
    * </p>
 *
 * @author ftm
 * @since 2024-07-11
 */
public interface ExcelMysqlMapper extends BaseMapper<ExcelMysql> {

    List<Map<String,Object>> listTable( @Param("table") String table,@Param("start") Long start,@Param("size") Long size);

    Long countTable( @Param("table") String table);
}