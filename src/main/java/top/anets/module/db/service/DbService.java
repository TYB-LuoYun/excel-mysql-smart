package top.anets.module.db.service;

import ch.qos.logback.core.util.TimeUtil;
import cn.hutool.core.convert.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import top.anets.exception.ServiceException;
import top.anets.module.base.WrapperQuery;
import top.anets.module.db.entity.AbaData;
import top.anets.module.db.entity.Abadata4;
import top.anets.module.ding.SendDingService;
import top.anets.module.ding.SendRequestParam;
import top.anets.utils.DateUtils;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
public class DbService {
    @Autowired
    private IAbadata4Service abadata4Service;
    @Autowired
    private IAbaDataService abaDataService;
    public ReentrantLock lock = new ReentrantLock();


    @Autowired
    private SendDingService sendDingService;


    public int moveToPg() {
        boolean b = lock.tryLock();
        if(!b){
            throw new ServiceException("正在执行中，请稍后");
        }
        try {
            int success =0;
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
                success+= collect.size();
                abaDataService.saveBatch(collect);
                System.out.println("当前进度:"+currentId+"/"+maxId);
            }
            return success;
        }catch (Exception e){
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }finally {
            lock.unlock();
        }
    }


    /**
     * 同步aba报表数据
     */
    @Transactional
    public Integer synchroDataOfAba()   {
        Calendar calendar = Calendar.getInstance();
        // 减去一个月
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date previousMonthDate = calendar.getTime();
        List<Map<String, Object>> time = abadata4Service.excuteSelect("SELECT max(日期范围) as Date   from abadata4");
        if(CollectionUtils.isEmpty(time)||time.get(0) == null){
//          不可能
            return null;
        }
        String format = DateUtils.getDate(previousMonthDate);
        Date maxDate = (Date) time.get(0).get("Date");
        String date1 = DateUtils.getDate(maxDate);
        if( date1.equals(format)){
            System.out.println("时间相等，说明同步过");
            return null;
        }


        List<Map<String, Object>> maps = abadata4Service.excuteSelect("SELECT max(CreateDate) as Date from repbrandsearchterms where date = '" + format + "'");
        if(CollectionUtils.isEmpty(maps)||  maps.get(0) == null){
//          说明还没有数据
            return null;
        }

        LocalDateTime date = (LocalDateTime) maps.get(0).get("Date");
        if(System.currentTimeMillis()-DateUtils.toDate(date).getTime() <  1000*60*60*24){
            System.out.println("源数据可能尚未更新完");
            return null;
        }

        Long maxId = abadata4Service.getMaxId();
        SendRequestParam.Text text = new SendRequestParam.Text();
        text.setContent("正在同步ABA数据:"+format);
        sendDingService.sendText(text);
        String sql1 ="INSERT INTO abadata4 \n" +
                "SELECT  \n" +
                " keyword.`searchTerm`AS'搜索词',\n" +
                " keyword.`searchFrequencyRank`AS'搜索频率排名',\n" +
                " keyword.`date`AS'日期范围',\n" +
                " keyword.`departmentName` AS'站点',\n" +
                " MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`clickedAsin` ELSE '' END ) '#1 已点击的 ASIN',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`clickedAsin` ELSE '' END ) '#2 已点击的 ASIN',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`clickedAsin` ELSE '' END ) '#3 已点击的 ASIN',\n" +
                "\n" +
                " MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`clickShare` ELSE '' END ) '#1 点击共享',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`clickShare` ELSE '' END ) '#2 点击共享',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`clickShare` ELSE '' END ) '#3 点击共享',\n" +
                " \n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`conversionShare` ELSE '' END ) '#1 转化共享',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`conversionShare` ELSE '' END ) '#2 转化共享',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`conversionShare` ELSE '' END ) '#3 转化共享',\n" +
                " \n" +
                "   MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`brand` ELSE '' END ) '1品牌',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`brand` ELSE '' END ) '2品牌',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`brand` ELSE '' END ) '3品牌',\n" +
                " \n" +
                "    MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`color` ELSE '' END ) '1颜色',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`color` ELSE '' END ) '2颜色',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`color` ELSE '' END ) '3颜色',\n" +
                " \n" +
                "    MAX(CASE keyword.`clickShareRank` WHEN '1' \n" +
                " THEN keyword.`size` ELSE '' END ) '1尺寸',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '2' \n" +
                " THEN keyword.`size` ELSE '' END ) '2尺寸',\n" +
                "  MAX(CASE keyword.`clickShareRank` WHEN '3' \n" +
                " THEN keyword.`size` ELSE '' END ) '3尺寸',\n" +
                " '' AS'cnWord',\n" +
                " keyword.id\n" +
                "\n" +
                " FROM (SELECT  \n" +
                "      statiscs.`repbrandsearchterms`.`searchTerm`,\n" +
                "      statiscs.`repbrandsearchterms`.`searchFrequencyRank`,\n" +
                "      statiscs.`repbrandsearchterms`.`date`,\n" +
                "      statiscs.`repbrandsearchterms`.`clickShareRank`,\n" +
                "      statiscs.`repbrandsearchterms`.`clickedAsin`,\n" +
                "      statiscs.`repbrandsearchterms`.`clickShare`,\n" +
                "      statiscs.`repbrandsearchterms`.`conversionShare`,\n" +
                "      statiscs.`repbrandsearchterms`.`departmentName`,\n" +
                "      SUBSTRING_INDEX(SUBSTRING_INDEX(JSON_EXTRACT(statiscs.`catalogitem`.`Cleaned`, \"$.brand\"),'\"',2),'\"',-1)AS'brand',\n" +
                "      SUBSTRING_INDEX(SUBSTRING_INDEX(JSON_EXTRACT(statiscs.`catalogitem`.`Cleaned`, \"$.color\"),'\"',2),'\"',-1)AS'color',\n" +
                "      SUBSTRING_INDEX(SUBSTRING_INDEX(JSON_EXTRACT(statiscs.`catalogitem`.`Cleaned`, \"$.size\"),'\"',2),'\"',-1)AS'size',\n" +
                "      NULL AS id\n" +
                "FROM statiscs.`repbrandsearchterms` LEFT JOIN statiscs.`catalogitem` ON\n" +
                " statiscs.`repbrandsearchterms`.`clickedAsin`=statiscs.`catalogitem`.`Asin` WHERE statiscs.`repbrandsearchterms`.`date`='"+format+"')keyword\n" +
                " GROUP BY keyword.searchFrequencyRank,keyword.searchTerm,keyword.date,keyword.departmentName; ";
        String sql2="UPDATE abadata4 \n" +
                "JOIN eskwordtrans \n" +
                "ON abadata4.搜索词 = eskwordtrans.OriginalWord\n" +
                "SET abadata4.cnWord = eskwordtrans.CnWord\n" +
                "WHERE abadata4.id  > "+maxId+";";
        Integer i = abadata4Service.excuteSql(sql1);
        Integer i1 = abadata4Service.excuteSql(sql2);
        return i;
    }
}
