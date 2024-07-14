package top.anets.module.excel.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TableService {
    void uploadExcelData(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap);

    List<String> selectTableColumn(String tableName,String tableSchema);

    void updateExcelTable(MultipartFile file, String tableName, Integer headRowNumber,Integer sheetIndex, Map fieldMap) throws IOException;
    void createExcelTable(MultipartFile file, String tableName, Integer headRowNumber,Integer sheetIndex) throws IOException;

    String getSimpleNameFromFile(String originalFilename);


    void newTableByFieldMap(LinkedHashMap<String, String> fieldMap, String tableName);
}
