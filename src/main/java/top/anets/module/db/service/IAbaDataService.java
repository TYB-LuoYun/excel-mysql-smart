package top.anets.module.db.service;

import top.anets.module.db.entity.AbaData;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ftm
 * @since 2024-07-23
 */
public interface IAbaDataService extends IService<AbaData> {


    Long getMaxId();
}
