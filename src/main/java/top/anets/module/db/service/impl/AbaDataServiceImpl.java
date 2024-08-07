package top.anets.module.db.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import top.anets.module.db.entity.AbaData;
import top.anets.module.db.mapper.AbaDataMapper;
import top.anets.module.db.service.IAbaDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.anets.support.datasources.Db;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ftm
 * @since 2024-07-23
 */
@Service
@DS(Db.Nodes)
public class AbaDataServiceImpl extends ServiceImpl<AbaDataMapper, AbaData> implements IAbaDataService {

    @Override
    public Long getMaxId() {
        return baseMapper.getMaxId();
    }
}
