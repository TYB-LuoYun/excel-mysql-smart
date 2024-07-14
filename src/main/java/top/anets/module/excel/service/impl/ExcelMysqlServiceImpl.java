package top.anets.module.excel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import top.anets.exception.ServiceException;
import top.anets.module.base.WrapperQuery;
import top.anets.module.excel.entity.ExcelMysql;
import top.anets.module.excel.mapper.ExcelMysqlMapper;
import top.anets.module.excel.service.IExcelMysqlService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.anets.module.excel.service.TableService;
import top.anets.module.excel.vo.ExcelMysqlVo;

import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ftm
 * @since 2024-07-11
 */
@Service
@Transactional
public class ExcelMysqlServiceImpl extends ServiceImpl<ExcelMysqlMapper, ExcelMysql> implements IExcelMysqlService {
    @Autowired
    private TableService tableService;

    @Override
    public IPage listTable(IPage page, String tableName) {
        Long count = baseMapper.countTable( "`"+tableName+"`");
        List<Map<String,Object>> data =  baseMapper.listTable( "`"+tableName+"`",(page.getCurrent()-1)*page.getSize(),page.getSize());
        page.setRecords(data);
        page.setTotal(count);
        return page;
    }

    @Override
    public void newExcelTable(ExcelMysqlVo excelMysqlVo) {
        LinkedHashMap<String, String> fieldMap = excelMysqlVo.getFieldMap();
        if(CollectionUtils.isEmpty(fieldMap)){
            throw new ServiceException("字段映射不能为空");
        }
//      去掉空值
        Iterator<Map.Entry<String, String>> iterator = fieldMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> next = iterator.next();
            if(StringUtils.isBlank(next.getValue())){
                iterator.remove();
            }
        }
        excelMysqlVo.setTableName(excelMysqlVo.getTableName().trim());
        tableService.newTableByFieldMap(fieldMap, excelMysqlVo.getTableName());
        excelMysqlVo.setCreateTime(new Date());
        excelMysqlVo.setUpdateTime(new Date());
        this.saveOrUpdate(WrapperQuery.from(excelMysqlVo,ExcelMysql.class));
    }
}
