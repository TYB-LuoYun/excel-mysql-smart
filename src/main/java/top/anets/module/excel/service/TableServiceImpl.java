package top.anets.module.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.util.StringUtils;
import io.swagger.models.auth.In;
import org.ehcache.core.util.CollectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import top.anets.exception.ServiceException;
import top.anets.module.excel.controller.ReadMergeAsMapListener;
import top.anets.module.excel.mapper.TableMapper;
import top.anets.utils.BathUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static io.netty.util.internal.SystemPropertyUtil.contains;

@Service
@Transactional
public class TableServiceImpl implements TableService{
    @Autowired
    private TableMapper tableMapper;
    @Override
    public void uploadExcelData(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap) {
//      先清空表
        tableMapper.excuteSql("delete from `"+tableName+"`");
        List<List<Map<Integer, Object>>> lists = BathUtil.pagingList(dataList, 2000);
        for(List<Map<Integer, Object>> item  : lists){
            String sql = genTableUpdateSql(headDataMap , item,  tableName,fieldMap );
            tableMapper.excuteSql(sql);
        }

    }

    @Override
    public List<String> selectTableColumn(String tableName, String tableSchema) {
        if(tableSchema == null){
            List<String> list = tableMapper.countTable(tableName);
            if(list.size()>1){
                 throw new ServiceException("存在重名表,请指定tableSchema");
            }
        }
        return tableMapper.selectTableColumn(tableName,tableSchema );
    }



    public void updateExcelTable(MultipartFile file,String tableName, Integer headRowNumber,Integer sheetIndex, Map fieldMap) throws IOException {

            if(StringUtils.isBlank(tableName)){
                tableName = getSimpleNameFromFile(file.getOriginalFilename());
            }
            List<String> stringObjectMap = this.selectTableColumn(tableName,null);
            if(headRowNumber == null){
                headRowNumber = 1;
            }
            ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();

            try(InputStream in = file.getInputStream()){
                EasyExcel.read(file.getInputStream()).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener).sheet(sheetIndex).headRowNumber(headRowNumber).doRead();
            }catch (Exception e){
                e.printStackTrace();
                throw new ServiceException(e.getMessage());
            }

            Map<Integer, String> headDataMap = readMergeAsMapListener.getHeadDataMap();
            if(CollectionUtils.isEmpty(fieldMap)){
                Iterator<Map.Entry<Integer, String>> iterator = headDataMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    boolean isFind = false;
                    Map.Entry<Integer, String> item = iterator.next();
                    if(stringObjectMap. contains(item.getValue())){
                        isFind = true;
                    }
                    if(!isFind){
                        iterator.remove(); // 安全地移除元素
                    }
                }
            }else{
//               去掉字段映射的空元素
                Iterator<Map.Entry<String, String>>  fm =fieldMap.entrySet().iterator();
                while(fm.hasNext()){
                    Map.Entry<String, String> item = fm.next();
                    if(StringUtils.isBlank(item.getKey())||StringUtils.isBlank(item.getValue())){
                        fm.remove();
                    }
                }
                Set set = fieldMap.keySet();
                Iterator<Map.Entry<Integer, String>> iterator = headDataMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, String> item = iterator.next();
                    if(!set.contains(item.getValue())){
                        iterator.remove(); // 安全地移除元素
                    }
                }
            }

            for (Map<Integer, Object> integerObjectMap : readMergeAsMapListener.getDataList()) {
//          每行
                Iterator<Map.Entry<Integer, Object>> it  = integerObjectMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Object> item = it.next();
                    if(!headDataMap.keySet().contains(item.getKey())){
                        it.remove();//删除行的某列
                    }
                }
            }

//          校验字段是否重复
            Collection values = fieldMap.values();
            Set<?> set = new HashSet<>(values);
            if(set.size()< values.size()){
                throw new ServiceException("列名重复，请检查");
            }
            this.uploadExcelData(headDataMap,readMergeAsMapListener.getDataList(),tableName,fieldMap);

    }

    public void createExcelTable(MultipartFile file, String tableName, Integer headRowNumber,Integer sheetIndex)   {

        ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
        try(InputStream in = file.getInputStream()){
            EasyExcel.read(in).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener).sheet(sheetIndex).headRowNumber(headRowNumber).doRead();
            String sql = genTableCreateSql(readMergeAsMapListener.getHeadDataMap(),tableName);
            tableMapper.excuteSql(sql);
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void newTableByFieldMap(LinkedHashMap<String, String> fieldMap, String tableName) {
        String sql = genTableCreateSql(fieldMap,tableName);
        tableMapper.excuteSql(sql);
    }


    public String getSimpleNameFromFile(String OriginalFilename){
        return  OriginalFilename.substring(0,OriginalFilename.lastIndexOf(".")).replaceAll("\\s+", "");
//                .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
    }




    private String genTableCreateSql(Map  field,String tableName) {
        String sql ="CREATE TABLE `"+tableName+"` ("  ;
        // 将 LinkedHashMap 的条目存储到 List 中
        List<Map.Entry> entryList = new ArrayList<>(field.entrySet());
        // 按照索引遍历 LinkedHashMap
        for (int i = 0; i < entryList.size(); i++) {
            Map.Entry  entry = entryList.get(i);
            sql+="`"+entry.getValue().toString().trim()+"` varchar(255) DEFAULT ''";
            if(i !=entryList.size() -1){
                sql+=",";
            }
        }
        sql+=") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        System.out.println(sql);
        return sql;
    }



    private String genTableUpdateSql(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap) {
//        String sqls ="INSERT INTO tableName (column1, column2, ...) VALUES (value1a, value2a, ...), (value1b, value2b, ...), ...;";
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `"+tableName+"` (");
        int count =0;
        if(CollectionUtils.isEmpty(fieldMap)){
            for (Map.Entry<Integer, String> entry : headDataMap.entrySet()) {
                sql.append("`"+entry.getValue()+"`");
                if(count <  headDataMap.size() -1){
                    sql.append(",");
                }
                count+=1;
            }
        }else{
            for (Map.Entry<Integer, String> entry : headDataMap.entrySet()) {
                sql.append("`"+fieldMap.get(entry.getValue())+"`");
                if(count <  headDataMap.size() -1){
                    sql.append(",");
                }
                count+=1;
            }
        }
        sql.append(") VALUES ");
        for(int i=0;i<dataList.size();i++){
            sql.append("(");
            Map<Integer, Object> row = dataList.get(i);
            int ct =0;
            if(row.size()<headDataMap.size()){
                List<Integer> keys = new ArrayList<>(row.keySet());
                Integer lastKey = keys.get(keys.size() - 1);
                for(int j=1;j<=headDataMap.size()-row.size();j++){
                    row.put(lastKey+j,"");
                }
            }
            for (Map.Entry<Integer, Object> entry : row.entrySet()) {
                if( entry.getValue()!=null){
                    sql.append("\""+entry.getValue().toString().trim().replaceAll("\"", "\\\\\"")+"\"");
                }else{
                    sql.append("''");
                }
                if(ct <  row.size() -1){
                    sql.append(",");
                }
                ct+=1;
            }
            sql.append(")");
            if(i< dataList.size()-1){
                sql.append(",");
            }

        }
        return sql.toString();
    }
}
