package top.anets.module.db.controller;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.anets.base.PageQuery;
import top.anets.base.QueryMap;
import top.anets.module.base.Fields;
import top.anets.module.base.WrapperQuery;
import top.anets.module.db.entity.AbaData;
import top.anets.module.db.entity.Abadata4;
import top.anets.module.db.service.IAbaDataService;
import top.anets.module.db.service.IAbadata4Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@RestController
@RequestMapping("/db")
public class DbController {
    @Autowired
    private IAbadata4Service abadata4Service;
    @Autowired
    private IAbaDataService abaDataService;

    @RequestMapping("move")
    public  void move(){
//        MongoDBUtil.saveBatch();
        Long targetStartId = abaDataService.getMaxId();
        if(targetStartId == null){
            targetStartId  = abadata4Service.getMinId();
        }
        Long currentId = targetStartId+1;
        Long maxId = abadata4Service.getMaxId();

        while (currentId < maxId){
            List<Long> numbers = LongStream.rangeClosed(currentId, currentId+2000).boxed().collect(Collectors.toList());
            List<Abadata4> abadata4s = abadata4Service.listByIds(numbers);
            currentId = currentId+2000+1;
            List<AbaData> collect = abadata4s.stream().map(e -> {
                AbaData from = WrapperQuery.from(e, AbaData.class);
                from.setClickShare1(Convert.toDouble(e.getClickShare1()));
                from.setTransformShare1(Convert.toDouble(e.getTransformShare1()));

                from.setClickShare2(Convert.toDouble(e.getClickShare2()));
                from.setTransformShare2(Convert.toDouble(e.getTransformShare2()));

                from.setClickShare3(Convert.toDouble(e.getClickShare3()));
                from.setTransformShare3(Convert.toDouble(e.getTransformShare3()));
                return from;
            }).collect(Collectors.toList());
            abaDataService.saveBatch(collect);
            System.out.println("当前进度:"+currentId+"/"+maxId);
        }
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
