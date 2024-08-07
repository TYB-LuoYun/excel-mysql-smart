package top.anets.module.db.service;

import top.anets.module.db.entity.Abadata4;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ftm
 * @since 2024-07-22
 */
public interface IAbadata4Service extends IService<Abadata4> {


    Long getMinId();

    Long getMaxId();
}
