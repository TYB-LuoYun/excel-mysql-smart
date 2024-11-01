package top.anets.module.excel.service;

import cn.hutool.core.convert.Convert;
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
import top.anets.cache.MemoryCache;
import top.anets.exception.ServiceException;
import top.anets.module.excel.controller.ReadMergeAsMapListener;
import top.anets.module.excel.mapper.TableMapper;
import top.anets.module.excel.model.ColumnInfo;
import top.anets.utils.BathUtil;
import top.anets.utils.DateUtils;
import top.anets.utils.RegexUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static io.netty.util.internal.SystemPropertyUtil.contains;

@Service
@Transactional
public class TableServiceImpl implements TableService{
    @Autowired
    private TableMapper tableMapper;

    @Resource
    private MemoryCache memoryCache;

    @Override
    public void uploadExcelData(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap,Integer mode,Map<Integer,ColumnInfo> dataConvert) {
        if(mode==null||mode!=1){
            //      先清空表
            tableMapper.excuteSql("delete from `"+tableName+"`");
        }

        List<List<Map<Integer, Object>>> lists = BathUtil.pagingList(dataList, 2000);
        int current =0;
        for(List<Map<Integer, Object>> item  : lists){
            String sql = genTableUpdateSql(headDataMap , item,  tableName,fieldMap,dataConvert );
            tableMapper.excuteSql(sql);
            current+=item.size();
            memoryCache.getCacheDay().put(tableName,0.3+0.7*current/dataList.size());
        }
    }

    @Override
    public void deleteTableData(String tableName) {
        tableMapper.excuteSql("delete from `"+tableName+"`");
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

    @Override
    public List< ColumnInfo> selectTableColumnInfo(String tableName, String tableSchema) {
        if(tableSchema == null){
            List<String> list = tableMapper.countTable(tableName);
            if(list.size()>1){
                throw new ServiceException("存在重名表,请指定tableSchema");
            }
        }

        return tableMapper.selectTableColumnInfo(tableName,tableSchema );
    }



    public void updateExcelTable(MultipartFile file,String tableName, Integer headRowNumber,Integer dataStartRowNum,Integer sheetIndex, Map fieldMap,Integer mode,List<ColumnInfo> needConvertType,Map<String,Object> customValue) throws IOException {

            if(StringUtils.isBlank(tableName)){
                tableName = getSimpleNameFromFile(file.getOriginalFilename());
            }
            List<String> stringObjectMap = this.selectTableColumn(tableName,null);
            if(headRowNumber == null){
                headRowNumber = 1;
            }
            ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
            readMergeAsMapListener.setDataStartRow(dataStartRowNum);
            memoryCache.getCacheDay().put(tableName,0.07);
            try(InputStream in = file.getInputStream()){
                EasyExcel.read(file.getInputStream()).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener)
                        .sheet(sheetIndex).headRowNumber(headRowNumber).doRead();
            }catch (Exception e){
                e.printStackTrace();
                throw new ServiceException(e.getMessage());
            }
            memoryCache.getCacheDay().put(tableName,0.2);
            Map<Integer, String> headDataMap = readMergeAsMapListener.getHeadDataMap();

           HashMap<Integer, Object> custom = new HashMap<>();
           if(fieldMap.keySet().contains("$dates")){
               Set<Integer> integers = headDataMap.keySet();
               int i = Collections.max(integers) + 1;
               headDataMap.put(i,"$dates");
               custom.put(i,customValue.get("$dates"));
            }


            if(CollectionUtils.isEmpty(fieldMap)){
                Iterator<Map.Entry<Integer, String>> iterator = headDataMap.entrySet().iterator();
                Set<String> valueSet = new HashSet<>();
                while (iterator.hasNext()) {
                    boolean isFind = false;
                    Map.Entry<Integer, String> item = iterator.next();
                    if(stringObjectMap. contains(item.getValue())){
                        isFind = true;
                    }
                    if(!isFind){
                        iterator.remove(); // 安全地移除元素
                    }

                    if(valueSet.contains(item.getValue())){
                        iterator.remove(); // 安全地移除重复值元素
                    }
                    valueSet.add(item.getValue());
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

                Set<String> valueSet = new HashSet<>();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, String> item = iterator.next();
                    if(!set.contains(item.getValue())){
                        iterator.remove(); // 安全地移除元素
                    }else if(valueSet.contains(item.getValue())){
                        iterator.remove(); // 安全地移除重复值元素
                    }
                    valueSet.add(item.getValue());
                }
            }
          /**
            * 获取数据没有的列
          */
            for (Map<Integer, Object> integerObjectMap : readMergeAsMapListener.getDataList()) {
//          每行
                Iterator<Map.Entry<Integer, Object>> it  = integerObjectMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Object> item = it.next();
                    if(!headDataMap.keySet().contains(item.getKey())){
                        it.remove();//删除行的某列
                    }
                }
//              补充缺失的长度
                if(integerObjectMap.size()<headDataMap.size()-custom.size()){
                    Integer max = Collections.max(integerObjectMap.keySet());
                    for(int j = 1 ;j<= headDataMap.size()-custom.size()-integerObjectMap.size();j++ ){
                        integerObjectMap.put(max+j,null);
                    }
                }
//              补充自定义字段
                if(!CollectionUtils.isEmpty(custom)){
                    integerObjectMap.putAll(custom);
                }
            }

//          校验字段是否重复
            Collection values = fieldMap.values();
            Set<?> set = new HashSet<>(values);
            if(set.size()< values.size()){
                throw new ServiceException("列名重复，请检查");
            }


//          数据类型处理
        List<Map<Integer, Object>> dataList = readMergeAsMapListener.getDataList();
        HashMap<Integer, ColumnInfo> dataConvert = new HashMap<>();
        for(ColumnInfo columnInfo: needConvertType){
                Integer index = -1;
                for (Map.Entry<Integer, String> entry : headDataMap.entrySet()) {
                    if(columnInfo.getColumnName().equals(fieldMap.get(entry.getValue()))){
                        index = entry.getKey();
                    }
                }
                if(index!=null&&index>=0){
                    columnInfo.setExcelColumnIndex(index);
                    dataConvert.put(index,columnInfo );
                }
        }

            memoryCache.getCacheDay().put(tableName,0.3);
            this.uploadExcelData(headDataMap,dataList,tableName,fieldMap,mode,dataConvert);

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

    @Override
    public  Map<String, Long> listPK(String tableName) {
        List<Map<String,Object>>   list = tableMapper.listPK(tableName);
        HashMap<String, Long> map = new HashMap<>();

        if(list!=null){
            list.stream().forEach(e->{
                map.put((String)e.get("Column_name"),(Long) e.get("Seq_in_index"));
            });
        }
        return  map;

    }

    @Override
    public void updatePK(Map<String, Long> keys, String tableName) {
        Map<String, Long> longMap = this.listPK(tableName);
        if(CollectionUtils.isEmpty(keys)&&CollectionUtils.isEmpty(longMap)){
            return;
        }

        if (keys.keySet().containsAll(longMap.keySet()) && longMap.keySet().containsAll(keys.keySet())) {
            return;
        } else {
            if(!CollectionUtils.isEmpty(longMap)){
                tableMapper.excuteSql("ALTER TABLE `"+tableName+"` drop primary key");
//                longMap.forEach((item,index)->{
//                    tableMapper.excuteSql("ALTER TABLE `"+tableName+"` MODIFY 列名 列类型 NULL;");
//                });
            }

            if(!CollectionUtils.isEmpty(keys)){
                String sql ="(";
                ArrayList<String> strings = new ArrayList<>(keys.keySet());
                for(int i=0;i<strings.size();i++){
                    sql += "`"+strings.get(i)+"`";
                    if(i<strings.size()-1){
                        sql+=",";
                    }
                }
                sql+=")";
                tableMapper.excuteSql("ALTER TABLE `"+tableName+"` ADD PRIMARY KEY "+sql);
            }
        }



    }

    @Override
    public void updateColumnType(String tableName, List<ColumnInfo> needConvert) {
        if(CollectionUtils.isEmpty(needConvert)){
            return;
        }
        needConvert.forEach(e->{
            String sql =null;
            if("varchar".equalsIgnoreCase(e.getDataType())){
                if(e.getLength() == null){
                    e.setLength(255);
                }
               sql="ALTER TABLE `"+tableName+"` MODIFY `"+e.getColumnName()+"` "+e.getDataType()+"("+e.getLength()+");";
           }else if("decimal".equalsIgnoreCase(e.getDataType())){
                sql="ALTER TABLE `"+tableName+"` MODIFY `"+e.getColumnName()+"` "+e.getDataType()+"(18,4);";
            }else{
                sql="ALTER TABLE `"+tableName+"` MODIFY `"+e.getColumnName()+"` "+e.getDataType()+";";
           }
            tableMapper.excuteSql(sql);
        });

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
            if("line_number".equalsIgnoreCase(entry.getValue().toString())){
                sql+="`"+entry.getValue().toString().trim()+"` bigint";
            }else{
                sql+="`"+entry.getValue().toString().trim()+"` varchar(255) DEFAULT ''";
            }
            if(i !=entryList.size() -1){
                sql+=",";
            }
        }
        sql+=") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        System.out.println(sql);
        return sql;
    }



    private String genTableUpdateSql(Map<Integer, String> headDataMap, List<Map<Integer, Object>> dataList,String tableName,Map<String,String> fieldMap,Map<Integer,ColumnInfo> dataConvert) {
//        String sqls ="INSERT INTO tableName (column1, column2, ...) VALUES (value1a, value2a, ...), (value1b, value2b, ...), ...;";
        StringBuilder sql = new StringBuilder();
        sql.append("REPLACE INTO `"+tableName+"` (");
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

//        Collection<ColumnInfo> values = dataConvert.values();
//        List< ColumnInfo> pkeys = new ArrayList<>();
//        values.forEach(item->{
//            if(item.ifPrimaryKey() && !"varchar".equalsIgnoreCase(item.getDataType())){
//                pkeys.add(item);
//            }
//        });
        for(int i=0;i<dataList.size();i++){
            Map<Integer, Object> row = dataList.get(i);
            /**
             * 获取有索引的列，如果非空，则跳过
             */
//            boolean isSkip = false;
//            for(ColumnInfo eve : pkeys){
//                if(row.get(eve.getExcelColumnIndex()) == null){
//                    isSkip = true;
//                }
//            }
//            if(isSkip){
//                continue;
//            }
            sql.append("(");
            int ct =0;
            if(row.size()<headDataMap.size()){
                List<Integer> keys = new ArrayList<>(row.keySet());
                Integer lastKey = keys.get(keys.size() - 1);
                Integer orginSize = row.size();
                for(int j=1;j<=headDataMap.size()-orginSize;j++){
                    row.put(lastKey+j,"");
                }
            }
            for (Map.Entry<Integer, Object> entry : row.entrySet()) {
                sql.append(getTypeValue(entry.getKey(),entry.getValue(),dataConvert.get(entry.getKey())));
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

    /**
     * 得到对应类型的值
     * @return
     */
    Object  getTypeValue(Integer key,Object o,ColumnInfo type){

        if(type == null || type.getDataType() == null){
            if(o!=null){
                return "\""+o.toString().trim().replaceAll("\"", "\\\\\"")+"\"";
            }else{
                return "''";
            }
        }
//                    转换
        if(o == null){
            if(type.getDataType().equalsIgnoreCase("varchar")){
                return "''";
            }else if(type.ifAllowNull()){
                return null;
            }
        }
        if(type.getDataType().equals("decimal")||type.getDataType().equals("double")){
            if(o instanceof String){
                if(((String) o).contains(",")){
                    o = ((String) o).replace(",", "");
                }
                if(((String) o).contains("%")){
                    o = ((String) o).replace("%", "");
                    o = Convert.toBigDecimal(o).divide(BigDecimal.valueOf(100));
                }else{
                    o = Convert.toBigDecimal(o);
                }
            }else{
                o = Convert.toBigDecimal(o);
            }
            if(type.getDataType().equals("double")){
                o = Convert.toDouble(o);
            }
        }else if(type.getDataType().equals("datetime")||type.getDataType().equalsIgnoreCase("date")){
            String dateTime = DateUtils.getDateTime(DateUtils.parseDateSmart(o));
            if(dateTime == null &&!type.ifAllowNull()){//不允许为null的话，给一个默认值
                throw new ServiceException("时间解析出错且不允许为null，请检查数据:"+o );
            }
            if(StringUtils.isNotBlank(dateTime)){
                o = "\""+dateTime+"\"";
            }else{
                o =null;
            }
        }else if(type.getDataType().equals("int")||type.getDataType().equals("bigint")){
            Long anInt = Convert.toLong(o);
            if(anInt == null){
                BigDecimal bigDecimal = RegexUtil.fetchNumber((String) o);
                if(bigDecimal !=null){
                    o = bigDecimal.longValue();
                }else{
                    o = null;
                }

            } else{
                o= anInt;
            }

            if(o==null){
                if(type.ifAllowNull()){
                    return  null;
                }else{
                    return 0;
                }
            }

        }else{
            o = "\""+o.toString().trim().replaceAll("\"", "\\\\\"")+"\"";
        }
        return  o;
    }



}
