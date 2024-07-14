package top.anets.module.excel.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.anets.exception.ServiceException;
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
     * 更新数据
     * @return
     */
    public ReentrantLock lock = new ReentrantLock();
    @RequestMapping("updateExcelTable")
    public void updateExcelTable(MultipartFile file,Integer id,Integer sheetIndex,String updateBy,@RequestPart(name = "fieldMap")  Map<String,String> fieldMap) throws IOException {
        if(CollectionUtils.isEmpty(fieldMap)){
            throw new ServiceException("字段映射不能为空");
        }
        boolean b = lock.tryLock();
        if(!b){
            throw new ServiceException("正在执行中，请稍后");
        }
        try{
            if(sheetIndex == null){
                sheetIndex = 0;
            }
            ReadSheet sheet = null;
            try (InputStream in = file.getInputStream()){
                sheet = EasyExcel.read(in).build().excelExecutor().sheetList().get(sheetIndex);
            }catch (Exception e){
                throw new ServiceException(e.getMessage());
            }
            ExcelMysql byId = excelMysqlService.getById(id);
            tableService.updateExcelTable(file,byId.getTableName(),byId.getExcelHeadRowNum(),sheetIndex,fieldMap);
            byId.setUpdateTime(new Date());
            byId.setExcelName(file.getOriginalFilename());
            byId.setSheetIndex(sheetIndex);
            byId.setSheetName(sheet.getSheetName());
            byId.setUpdateBy(updateBy);
            excelMysqlService.saveOrUpdate(byId);
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
    public Object getFieldMap(MultipartFile file , @NotNull Integer id, @NotNull  Integer sheetIndex)  {
        ExcelMysql byId = excelMysqlService.getById(id);
        Map<Integer, String> headDataMap = null;
        ReadMergeAsMapListener readMergeAsMapListener = new ReadMergeAsMapListener();
        readMergeAsMapListener.setLimitRow(byId.getExcelHeadRowNum());
        try(InputStream in = file.getInputStream()){
            EasyExcel.read(in).extraRead(CellExtraTypeEnum.MERGE).registerReadListener(readMergeAsMapListener).sheet(sheetIndex).headRowNumber(byId.getExcelHeadRowNum()).doRead();
            headDataMap = readMergeAsMapListener.getHeadDataMap();
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
//      获取表的所有字段
        List<String> list = tableService.selectTableColumn(byId.getTableName(), null);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for(Map.Entry<Integer,String> item : headDataMap.entrySet()){
            String value = item.getValue();
            AtomicReference<String> matchValue = new AtomicReference<>();
//          精确匹配
            if(list.contains(value)){
                matchValue.set(value);
            }
//          模糊匹配
            if(matchValue.get() ==null){
                list.forEach(e->{
                    if(e.contains(value) || (value!=null && value.contains(e))){
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
        object.put("fieldMap",linkedHashMap);
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
