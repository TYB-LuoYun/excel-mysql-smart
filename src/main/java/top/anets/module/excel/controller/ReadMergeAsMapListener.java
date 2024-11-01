package top.anets.module.excel.controller;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisStopException;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.util.StringUtils;
import lombok.Data;
import top.anets.exception.ServiceException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ReadMergeAsMapListener extends AnalysisEventListener<Map<Integer, Object>> {
    private final List<Map<Integer, Object>> dataList = new ArrayList<Map<Integer, Object>>();
    private final Map<Integer, String> headDataMap = new LinkedHashMap<>();
    private final List<CellExtra> extraList = new ArrayList<CellExtra>();
    private final List<Map<Integer, Object>> headList = new ArrayList<Map<Integer, Object>>();
    private int headRowCount = 0;
    private Integer dataStartRow = null;
    private Integer limitRow = null;
    private AtomicInteger countReaded =  new AtomicInteger(0);

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        super.invokeHead(headMap, context);



        HashMap<Integer, Object> map = new LinkedHashMap<>();
        map.put(-1,"line_number");
        for(Integer key : headMap.keySet()){
            ReadCellData cellData = headMap.get(key);
            String value = cellData.getStringValue();
            String head = headDataMap.get(key);
            map.put(key,value);
        }
        headList.add(map);
        // 记录表头行数
        headRowCount++;
        countReaded.incrementAndGet();
    }


    public void invoke(Map<Integer, Object> data, AnalysisContext context) {

        if(limitRow!=null&&limitRow<=countReaded.get()){
            this.doAfterAllAnalysed(null);
            throw new ExcelAnalysisStopException("停止读取");
        }

        int rowIndex = context.readRowHolder().getRowIndex() + 1;
        if(dataStartRow!=null&&dataStartRow>rowIndex){
            return;
        }

        Map<Integer, Object> map = new LinkedHashMap<>();
        map.put(-1,rowIndex);
        map.putAll(data);
        dataList.add(map);
//        dataList.add(data);
        countReaded.incrementAndGet();

    }


    public void extra(CellExtra extra, AnalysisContext context) {
        CellExtraTypeEnum type = extra.getType();
        // 合并
        if (type.equals(CellExtraTypeEnum.MERGE)){
            extraList.add(extra);
        }
    }

    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("read finish");
//      合并表头
//        fillDataListByMerge(headList,extraList,0);
        HashMap<Integer, LinkedHashSet<String>> setHashMap = new HashMap<>();
//      合并值
        headList.forEach(item->{
            for(Map.Entry<Integer,Object> each : item.entrySet()){
                Integer key = each.getKey();
                String value = (String) each.getValue();
                if(value!=null){
//                    去掉空格与换行
                    value = value.replaceAll("\\s+", "_");
                }
                LinkedHashSet<String> strings = setHashMap.get(key);
                if(strings == null){
                    if(StringUtils.isNotBlank(value)){
                        strings = new LinkedHashSet<>();
                        strings.add(value);
                    }
                }else{
                    if(StringUtils.isNotBlank(value)){
                        strings.add(value);
                    }
                }
                setHashMap.put(key,strings);
            }
        });

        for(Map.Entry<Integer,LinkedHashSet<String>> each : setHashMap.entrySet()){
            if(each.getValue()!=null){
                headDataMap.put(each.getKey(),String.join("_",each.getValue()));
            }
        }

//      合并内容
        fillDataListByMerge(dataList,extraList,headRowCount);
    }








    /**
     * 和
     * @param dataList
     * @param extraList
     * @param headRow
     *
     * 如果headRow为0的话，那么会处理 头的单元格合并
     */
    private static void fillDataListByMerge(List<Map<Integer, Object>> dataList, List<CellExtra> extraList, Integer headRow) {
        for (CellExtra cellExtra : extraList) {
            Integer rowIndex = cellExtra.getRowIndex();
            if (rowIndex < headRow){
                continue;
            }
            int dataListIndex = rowIndex - headRow;
            if(dataListIndex>=dataList.size()){
                continue;
            }
            Integer dataMapKey = cellExtra.getColumnIndex();
            Map<Integer, Object> dataMap = dataList.get(dataListIndex);
            Integer firstRowIndex = cellExtra.getFirstRowIndex() - headRow;
            Integer lastRowIndex = cellExtra.getLastRowIndex() - headRow;
            Integer firstColumnIndex = cellExtra.getFirstColumnIndex();
            Integer lastColumnIndex = cellExtra.getLastColumnIndex();
            Object value = dataMap.get(dataMapKey);
            // 左右合并
            for (int i = firstColumnIndex + 1; i < lastColumnIndex + 1; i++) {
                dataMap.put(i, value);
            }
            // 上下合并
            for (int i = firstRowIndex + 1; i < lastRowIndex + 1; i++) {
                if(i>=dataList.size()){
                    continue;
                }
                Map<Integer, Object> integerObjectMap = dataList.get(i);
                integerObjectMap.put(firstColumnIndex, value);
                if (!firstColumnIndex.equals(lastColumnIndex)){
                    for (int j = firstColumnIndex + 1; j < lastColumnIndex + 1; j++) {
                        integerObjectMap.put(j, value);
                    }
                }
            }
        }
    }



}
