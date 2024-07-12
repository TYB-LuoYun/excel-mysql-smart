package top.anets.module.excel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.anets.module.excel.entity.ExcelMysql;
import top.anets.module.excel.mapper.ExcelMysqlMapper;
import top.anets.module.excel.service.IExcelMysqlService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ftm
 * @since 2024-07-11
 */
@Service
public class ExcelMysqlServiceImpl extends ServiceImpl<ExcelMysqlMapper, ExcelMysql> implements IExcelMysqlService {

    @Override
    public IPage listTable(IPage page, String tableName) {
        Long count = baseMapper.countTable( "`"+tableName+"`");
        List<Map<String,Object>> data =  baseMapper.listTable( "`"+tableName+"`",(page.getCurrent()-1)*page.getSize(),page.getSize());
        page.setRecords(data);
        page.setTotal(count);
        return page;
    }
}
