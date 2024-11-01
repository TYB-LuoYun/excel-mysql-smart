package top.anets.module.excel.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.anets.cache.MemoryCache;
import top.anets.exception.ServiceException;
import top.anets.module.excel.model.ColumnInfo;
import top.anets.module.excel.service.IExcelMysqlService;
import top.anets.module.excel.entity.ExcelMysql;
import top.anets.module.excel.service.TableService;
import top.anets.module.excel.vo.ExcelMysqlVo;
import top.anets.module.base.WrapperQuery;
import top.anets.module.base.PageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ftm
 * @since 2024-07-11
 */
@Api(tags = {""})
@RestController
@RequestMapping("/excel-mysql")
@Validated
public class ExcelMysqlController {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    private IExcelMysqlService excelMysqlService;


    @Resource
    private TableService tableService;



    @Resource
    private MemoryCache memoryCache;

    /**
     * 创建表
     * @param file
     * @param tableName
     * @return
     * @throws IOException
     */
    @RequestMapping("createExcelTable")
    public void createExcelTable(MultipartFile file,String tableName,Integer headRowNumber,Integer sheetIndex) throws IOException {
        if(StringUtils.isBlank(tableName)){
            tableName = tableService.getSimpleNameFromFile(file.getOriginalFilename());
        }
        if(sheetIndex == null){
            sheetIndex = 0;
        }
        tableName = "e_"+tableName;
        /**
         * 查询表名是否存在
         */
        long count = excelMysqlService.count(Wrappers.<ExcelMysql>lambdaQuery().eq(ExcelMysql::getTableName, tableName));
        if(count>0){
            throw new ServiceException("表已存在");
        }
        if(headRowNumber == null){
            headRowNumber = 1;
        }
        ZipSecureFile.setMaxFileCount(20000);
        tableService.createExcelTable(file,tableName,headRowNumber,sheetIndex);
        ExcelMysql excelMysql = new ExcelMysql();
        excelMysql.setTableName(tableName);
        excelMysql.setExcelName(file.getOriginalFilename());
        excelMysql.setSheetIndex(0);
        excelMysql.setExcelHeadRowNum(headRowNumber);
        excelMysql.setCreateTime(new Date());
        excelMysql.setUpdateTime(new Date());
        excelMysqlService.saveOrUpdate(excelMysql);
    }


    @RequestMapping("newExcelTable")
    public void newExcelTable(@RequestBody ExcelMysqlVo excelMysqlVo)  {
        /**
         * 查询表名是否存在
         */
        long count = excelMysqlService.count(Wrappers.<ExcelMysql>lambdaQuery().eq(ExcelMysql::getTableName, excelMysqlVo.getTableName().trim()));
        if(count>0){
            throw new ServiceException("表已存在");
        }
        excelMysqlService.newExcelTable(excelMysqlVo);
    }


    @RequestMapping("previewCreateTable")
    public Object previewCreateTable(MultipartFile file,String tableName,Integer headRowNumber,Integer sheetIndex)  {
        if(StringUtils.isBlank(tableName)){
            tableName = tableService.getSimpleNameFromFile(file.getOriginalFilename());
        }
        if(sheetIndex == null){
            sheetIndex = 0;
        }
        if(!tableName.startsWith("e_")){
            tableName = "e_"+tableName;
        }

        if(headRowNumber == null){
            headRowNumber = 1;
        }
        ZipSecureFile.setMaxFileCount(20000);
        ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
        readMergeAsMapListener.setLimitRow(headRowNumber);
        List<ReadSheet> sheets = null;
        try(InputStream in = file.getInputStream()){
            try(ExcelReader excelReader = EasyExcel.read(in, readMergeAsMapListener).extraRead(CellExtraTypeEnum.MERGE).build();){
                //            excelReader.finish();
                sheets = excelReader.excelExecutor().sheetList();
                // 构建一个sheet 这里可以指定名字或者no
                ReadSheet readSheet = EasyExcel.readSheet(sheetIndex).headRowNumber(headRowNumber).build();
                // 读取一个sheet
                excelReader.read(readSheet);
                // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的,有close了就不用管
//                excelReader.finish();
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }

        /**
         * 获取字段映射
         */
        Map<Integer, String> headDataMap = readMergeAsMapListener.getHeadDataMap();
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for(Map.Entry<Integer,String> item : headDataMap.entrySet()){
            if(StringUtils.isNotBlank(item.getValue())){
                linkedHashMap.put(item.getValue(), item.getValue());
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tableName", tableName);
        jsonObject.put("sheetIndex", sheetIndex);
        jsonObject.put("excelHeadRowNum", headRowNumber);
        jsonObject.put("sheets",sheets  );
        jsonObject.put("fieldMap", linkedHashMap );
        return jsonObject;
    }

    /**
     * 获取进度
     */
    @RequestMapping("getProcess")
    public Object getProcess(String tableName){
        return memoryCache.getCacheDay().getIfPresent(tableName);
    }


    /**
     * 更新数据
     * @return
     */
    public ReentrantLock lock = new ReentrantLock();
    @RequestMapping("updateExcelTable")
    public void updateExcelTable(MultipartFile file,Integer id,Integer sheetIndex,Integer excelHeadRowNum,Integer dataStartRowNum,String updateBy,@RequestPart(name = "fieldMap")  Map<String,String> fieldMap,@RequestPart(name = "keys")  Map<String,Long> keys,Integer mode,@RequestPart(name = "columnInfo") List<ColumnInfo> columnInfo ,@RequestPart(name = "custom")  Map<String,Object> custom) throws IOException {
        if(CollectionUtils.isEmpty(fieldMap)){
            throw new ServiceException("字段映射不能为空");
        }
        boolean b = lock.tryLock();
        if(!b){
            throw new ServiceException("正在执行中，请稍后");
        }
        try{
            ExcelMysql byId = excelMysqlService.getById(id);
            memoryCache.getCacheDay().put(byId.getTableName(),0);
            List<ColumnInfo> columnInfos = tableService.selectTableColumnInfo(byId.getTableName(), null);
            List<ColumnInfo> needConvert =  new ArrayList<>();

            for (ColumnInfo each : columnInfo) {
                boolean isDistinct = false;
                for (ColumnInfo item : columnInfos) {
                    if(item.getColumnName().equalsIgnoreCase(each.getColumnName())&& !item.getDataType().equals(each.getDataType())){
                        isDistinct = true;
                        break;
                    }
                }
                if(isDistinct){
                    needConvert.add(each);
                }
            }

            try {
                if(mode==null||mode!=1){
                    tableService.deleteTableData(byId.getTableName());
                }
//              更改字段类型
                tableService.updateColumnType(byId.getTableName(),needConvert);
//              为了能加索引，最好先删除表
                tableService.updatePK(keys,byId.getTableName());
//              可能需要更改数据类型
            }catch (Exception e){
                e.printStackTrace();
                throw  new ServiceException("修改索引或者类型失败，建议选用覆盖模式");
            }
            if(sheetIndex == null){
                sheetIndex = 0;
            }
            ZipSecureFile.setMaxFileCount(20000);
            ReadSheet sheet = null;
            try (InputStream in = file.getInputStream()){
                sheet = EasyExcel.read(in).build().excelExecutor().sheetList().get(sheetIndex);
            }catch (Exception e){
                throw new ServiceException(e.getMessage());
            }

            memoryCache.getCacheDay().put(byId.getTableName(),0.05);

            if(excelHeadRowNum!=null){
                byId.setExcelHeadRowNum(excelHeadRowNum);
                if(dataStartRowNum == null || dataStartRowNum<=excelHeadRowNum){
                    dataStartRowNum = excelHeadRowNum+1;
                }
            }
            if(dataStartRowNum !=null){
                byId.setDataStartRowNum(dataStartRowNum);
            }
            /**
             * 由于excel解析的都是字符串，菲字符串的要转换类型
             */
            List<ColumnInfo> notStringType =  new ArrayList<>();
            List<ColumnInfo> columnInfoNew = tableService.selectTableColumnInfo(byId.getTableName(), null);
            columnInfoNew.forEach(item->{
                if(!"varchar".equalsIgnoreCase(item.getDataType())){
                    notStringType.add(item);
                }
            });
            memoryCache.getCacheDay().put(byId.getTableName(),0.06);
            tableService.updateExcelTable(file,byId.getTableName(),byId.getExcelHeadRowNum(),byId.getDataStartRowNum(),sheetIndex,fieldMap,mode,notStringType,custom);
            byId.setUpdateTime(new Date());
            byId.setExcelName(file.getOriginalFilename());
            byId.setSheetIndex(sheetIndex);
            byId.setSheetName(sheet.getSheetName());
            byId.setUpdateBy(updateBy);
            excelMysqlService.saveOrUpdate(byId);
            memoryCache.getCacheDay().put(byId.getTableName(),1);
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }finally {
            lock.unlock();
        }
    }


    /**
     * 获取sheet
     * @param
     * @return
     */
    @RequestMapping("getSheets")
    public Object getSheet(MultipartFile file ) {
        List<ReadSheet> readSheets = null;
        try (InputStream in = file.getInputStream()){
            ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
            readMergeAsMapListener.setLimitRow(0);
            ZipSecureFile.setMaxFileCount(20000);
            try(ExcelReader excelReader = EasyExcel.read(in).build()){
                readSheets = excelReader.excelExecutor().sheetList();
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
        return readSheets;
    }




    /**
     * 获取sheet的字段映射
     * @param id
     * @return
     */
    @RequestMapping("getFieldMap")
    public Object getFieldMap(MultipartFile file , @NotNull Integer id, @NotNull  Integer sheetIndex,Integer excelHeadRowNum,Integer dataStartRowNum)  {
        ExcelMysql byId = excelMysqlService.getById(id);
        Map<Integer, String> headDataMap = null;
        /**
         * 查看表主键
         */
        Map<String,Long> keys =tableService.listPK(byId.getTableName());
        ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
        if(excelHeadRowNum == null){
            excelHeadRowNum = byId.getExcelHeadRowNum();
        }
        if(dataStartRowNum == null){
            dataStartRowNum = byId.getDataStartRowNum();
        }
        if(excelHeadRowNum!=null){
            if(dataStartRowNum == null || dataStartRowNum<=excelHeadRowNum){
                dataStartRowNum = excelHeadRowNum+1;
            }
        }
        readMergeAsMapListener.setLimitRow(excelHeadRowNum);
        ZipSecureFile.setMaxFileCount(20000);
        try(InputStream in = file.getInputStream()){
            EasyExcel.read(in).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener).sheet(sheetIndex).headRowNumber(excelHeadRowNum).doRead();
            headDataMap = readMergeAsMapListener.getHeadDataMap();
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
//      获取表的所有字段信息
        List<ColumnInfo> list2 = tableService.selectTableColumnInfo(byId.getTableName(), null);
        List<String> list =tableService.selectTableColumn(byId.getTableName(),null);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for(Map.Entry<Integer,String> item : headDataMap.entrySet()){
            String value = item.getValue();
            AtomicReference<String> matchValue = new AtomicReference<>();
//          精确匹配
            if(list.contains(value)){
//              计算相似度
                matchValue.set(value);
            }
//          模糊匹配
            if(matchValue.get() ==null){
                list.forEach(e->{
                    if(matchValue.get() == null && (e.contains(value) || (value!=null && value.contains(e)))){
                        matchValue.set(e);
                    }
                });
            }
            if(matchValue.get() !=null){
                linkedHashMap.put(value,matchValue.get());
            }else{
                linkedHashMap.put(value,null);
            }
        }
        JSONObject object = new JSONObject();
        object.put("columns",list);
        object.put("columnInfo",list2);
        object.put("fieldMap",linkedHashMap);
        object.put("tableName",byId.getTableName());
        object.put("excelHeadRowNum",excelHeadRowNum);
        object.put("dataStartRowNum",dataStartRowNum );
        object.put("keys",keys);
        object.put("dataTypes",Arrays.asList("varchar","int","decimal","datetime","date","double","bigint","text"));
        return object;
    }



    @RequestMapping("getFieldMaps")
    public Object getFieldMaps(MultipartFile file , @NotNull Integer id, @NotNull  Integer sheetIndex,Integer excelHeadRowNum,Integer dataStartRowNum)  {
        ExcelMysql byId = excelMysqlService.getById(id);
        Map<Integer, String> headDataMap = null;
        /**
         * 查看表主键
         */
        Map<String,Long> keys =tableService.listPK(byId.getTableName());
        ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
        if(excelHeadRowNum == null){
            excelHeadRowNum = byId.getExcelHeadRowNum();
        }
        if(dataStartRowNum == null){
            dataStartRowNum = byId.getDataStartRowNum();
        }
        if(excelHeadRowNum!=null){
            if(dataStartRowNum == null || dataStartRowNum<=excelHeadRowNum){
                dataStartRowNum = excelHeadRowNum+1;
            }
        }
        readMergeAsMapListener.setLimitRow(excelHeadRowNum);
        ZipSecureFile.setMaxFileCount(20000);
        try(InputStream in = file.getInputStream()){
            EasyExcel.read(in).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener).sheet(sheetIndex).headRowNumber(excelHeadRowNum).doRead();
            headDataMap = readMergeAsMapListener.getHeadDataMap();
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
//      获取表的所有字段信息
        List<ColumnInfo> list2 = tableService.selectTableColumnInfo(byId.getTableName(), null);
        List<String> list = tableService.selectTableColumn(byId.getTableName(),null);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        Collection<String> values = headDataMap.values();
        for( ColumnInfo  item : list2){
            String value = item.getColumnName();
            AtomicReference<String> matchValue = new AtomicReference<>();
//          精确匹配
            if(values.contains(value)){
//              计算相似度
                matchValue.set(value);
            }
//          模糊匹配
            if(matchValue.get() ==null){
                values.forEach(e->{
                    if(matchValue.get() == null && (e.contains(value) || (value!=null && value.contains(e)))){
                        matchValue.set(e);
                    }
                });
            }
            if(matchValue.get() !=null){
                linkedHashMap.put(matchValue.get(),value);
            }else{
                if(value.equalsIgnoreCase("$dates")){
                    linkedHashMap.put("$dates",value);
                }else if(value.equalsIgnoreCase("$update_time")){
                    linkedHashMap.put("$update_time",value);
                }else{
                    linkedHashMap.put("",value);
                }
            }
        }
        JSONObject object = new JSONObject();
        object.put("columns",list);
        object.put("columnInfo",list2);
        object.put("columnExcel",values);


        LinkedHashMap<String, String> newMap = new LinkedHashMap<>();
        if (linkedHashMap.containsKey("$dates")) {
            newMap.put("$dates", linkedHashMap.remove("$dates"));
        }
        if(linkedHashMap.containsValue("$update_time")){
            linkedHashMap.remove("$update_time");
        }
        linkedHashMap.forEach(newMap::put);  // 将剩余元素加入新map
        linkedHashMap.clear();               // 清空原map
//        linkedHashMap.putAll(newMap);        // 复制回原map

        object.put("fieldMap",newMap);
        object.put("tableName",byId.getTableName());
        object.put("excelHeadRowNum",excelHeadRowNum);
        object.put("dataStartRowNum",dataStartRowNum );
        object.put("keys",keys);
        HashMap<String, Object> map = new HashMap<>();
        map.put("$dates",null);
        object.put("custom",map);
        object.put("dataTypes",Arrays.asList("varchar","int","decimal","datetime","date","double","bigint","text"));
        return object;
    }



    @ApiOperation(value = "Id查询")
    @GetMapping("/detail/{id}")
    public ExcelMysql findById(@PathVariable Long id){
        return excelMysqlService.getById(id);
    }


    @ApiOperation(value = "查询-分页")
    @RequestMapping("lists")
    public IPage lists(  ExcelMysqlVo excelMysqlVo, PageQuery query){
        IPage  pages = excelMysqlService.page(query.Page(),WrapperQuery.query(excelMysqlVo));
        WrapperQuery.ipage(pages,ExcelMysqlVo.class).getRecords().forEach(item->{
        //         todo    item.get...
 
        });
        return pages;
    }



    @RequestMapping("listTable")
    public JSONObject listTable(PageQuery query, Integer id){
        ExcelMysql byId = excelMysqlService.getById(id);
        List<String> stringObjectMap = tableService.selectTableColumn(byId.getTableName(),null);
        IPage  pages = excelMysqlService.listTable(query.Page(),byId.getTableName());
        JSONObject object = new JSONObject();
        object.put("head",stringObjectMap);
        object.put("data",pages);
        return object;
    }

}
