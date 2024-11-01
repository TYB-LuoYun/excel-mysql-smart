package top.anets.module.excel.mapper;

import org.apache.ibatis.annotations.*;
import top.anets.module.excel.model.ColumnInfo;

import java.util.List;
import java.util.Map;

@Mapper
public interface TableMapper {
    @Update("${sql}")
    void excuteSql(@Param("sql") String sql);

    List< ColumnInfo> selectTableColumnInfo(@Param("tableName")String tableName, @Param("tableSchema")String tableSchema);
     List< String  > selectTableColumn( @Param("tableName") String tableName ,@Param("tableSchema") String tableSchema);
    List< String  > countTable ( @Param("tableName") String tableName);

    @Select("SHOW KEYS FROM `${tableName}` WHERE Key_name = 'PRIMARY'")
    List<Map<String, Object>> listPK(@Param("tableName")String tableName);


}
