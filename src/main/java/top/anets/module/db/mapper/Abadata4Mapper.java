package top.anets.module.db.mapper;

import org.apache.ibatis.annotations.Select;
import top.anets.module.db.entity.Abadata4;
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
 * @since 2024-07-22
 */
public interface Abadata4Mapper extends BaseMapper<Abadata4> {
    @Select("select min(id) from abadata4")
    Long getMinId();

    @Select("select max(id) from abadata4")
    Long getMaxId();
}