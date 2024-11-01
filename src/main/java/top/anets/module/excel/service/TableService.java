package top.anets.module.excel.service;

import org.springframework.web.multipart.MultipartFile;
import top.anets.module.excel.model.ColumnInfo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TableService {
    void uploadExcelData(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap,Integer mode,Map<Integer,ColumnInfo> dataConvert);
    void deleteTableData(String tableName);
    List<String> selectTableColumn(String tableName,String tableSchema);
    List< ColumnInfo> selectTableColumnInfo(String tableName,String tableSchema);

    void updateExcelTable(MultipartFile file, String tableName, Integer headRowNumber,Integer dataStartRowNum,Integer sheetIndex, Map fieldMap,Integer mode,List<ColumnInfo> needConvertType,Map<String,Object> custom) throws IOException;
    void createExcelTable(MultipartFile file, String tableName, Integer headRowNumber,Integer sheetIndex) throws IOException;

    String getSimpleNameFromFile(String originalFilename);


    void newTableByFieldMap(LinkedHashMap<String, String> fieldMap, String tableName);

    Map<String, Long> listPK(String tableName);

    void updatePK(Map<String, Long> keys, String tableName);


    void updateColumnType(String tableName, List<ColumnInfo> needConvert);
}
