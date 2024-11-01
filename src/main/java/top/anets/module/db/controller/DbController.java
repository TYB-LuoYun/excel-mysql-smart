package top.anets.module.db.controller;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.anets.base.PageQuery;
import top.anets.base.QueryMap;
import top.anets.exception.ServiceException;
import top.anets.module.base.Fields;
import top.anets.module.base.WrapperQuery;
import top.anets.module.db.entity.AbaData;
import top.anets.module.db.entity.Abadata4;
import top.anets.module.db.service.DbService;
import top.anets.module.db.service.IAbaDataService;
import top.anets.module.db.service.IAbadata4Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@RestController
@Slf4j
@RequestMapping("/db")
public class DbController {

    @Autowired
    private DbService dbService;

    @RequestMapping("move")
    public  void move(){
        dbService.moveToPg();
    }


//    @RequestMapping("query")
//    public IPage query(PageQuery pageQuery){
//        long start = System.currentTimeMillis();
//        QueryMap like = QueryMap.build().like(Fields.name(AbaDataMongo::getSearchTerms), "躺椅");
//        IPage<AbaDataMongo> page = MongoDBUtil.page(pageQuery.Page(), WrapperQueryForMongo.query(like), AbaDataMongo.class);
//        System.out.println("耗时:"+(System.currentTimeMillis()-start));
//        return page;
//    }
}
