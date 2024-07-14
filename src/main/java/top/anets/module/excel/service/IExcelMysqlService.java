package top.anets.module.excel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.anets.module.excel.entity.ExcelMysql;
import com.baomidou.mybatisplus.extension.service.IService;
import top.anets.module.excel.vo.ExcelMysqlVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ftm
 * @since 2024-07-11
 */
public interface IExcelMysqlService extends IService<ExcelMysql> {


    IPage listTable(IPage page, String tableName);

    void newExcelTable(ExcelMysqlVo excelMysqlVo);
}
