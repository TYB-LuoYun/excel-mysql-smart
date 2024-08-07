package top.anets.module.db.mapper;

import org.apache.ibatis.annotations.Select;
import top.anets.module.db.entity.AbaData;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
    * mapper接口
    * </p>
 *
 * @author ftm
 * @since 2024-07-23
 */
public interface AbaDataMapper extends BaseMapper<AbaData> {

    @Select("select max(id) from aba_data")
    Long getMaxId();

}