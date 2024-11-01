package top.anets.module.db.service.impl;

import top.anets.module.db.entity.Abadata4;
import top.anets.module.db.mapper.Abadata4Mapper;
import top.anets.module.db.service.IAbadata4Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ftm
 * @since 2024-07-22
 */
@Service
public class Abadata4ServiceImpl extends ServiceImpl<Abadata4Mapper, Abadata4> implements IAbadata4Service {

    @Override
    public Long getMinId() {
        return baseMapper.getMinId();
    }

    @Override
    public Long getMaxId() {
        return baseMapper.getMaxId();
    }

    @Override
    public Integer excuteSql(String sql) {
       return baseMapper.excuteSql(sql);
    }

    @Override
    public List<Map<String, Object>> excuteSelect(String sql) {
        return baseMapper.excuteSelect(sql);
    }
}
