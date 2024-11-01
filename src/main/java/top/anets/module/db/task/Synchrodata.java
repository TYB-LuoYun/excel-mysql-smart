//package top.anets.module.db.task;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import top.anets.exception.ServiceException;
//import top.anets.module.db.controller.DbController;
//import top.anets.module.db.service.DbService;
//import top.anets.module.ding.SendDingService;
//import top.anets.module.ding.SendRequestParam;
//
//import java.util.concurrent.locks.ReentrantLock;
//
//@RefreshScope
//@Component
//@Slf4j
//public class Synchrodata {
//    @Autowired
//    private DbService dbService;
//
//    @Autowired
//    private SendDingService sendDingService;
//
//
//    public ReentrantLock lock = new ReentrantLock();
//
//    public ReentrantLock lock2 = new ReentrantLock();
//    int trys=0;
//    /**
//     * 关键词报表数据同步数据到PG
//     */
//    //0 */1 * * * ? 每分钟执行一次
////    0/10 * * * * ?  每10s 执行一次
//    @Scheduled(cron = "0 0 0/4 * * ?")
//    @Async
//    public void toPg(){
//        boolean b = lock.tryLock();
//        if(!b){
//            log.info("任务正在执行，请稍后");
//            return;
//        }
//        try {
//            log.info("开始同步pg");
//            try {
//                int i = dbService.moveToPg();
//                if(i>0){
//                    sendDing("成功同步pg数据"+i+"条");
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//                String msg = "出现异常,"+e.getMessage()+",重试:"+trys;
//                log.info(msg);
//                sendDing(msg);
//                trys++;
//                if(trys<5){
//                     this.toPg();
//                }else{
//                    trys = 0;
//                }
//            }
//
//        }catch (Exception e){
//        }finally {
//            lock.unlock();
//            log.info("同步pg任务结束");
//        }
//    }
//
//
//    /**
//     * 关键词报表数据拉取
//     */
//    @Scheduled(cron = "0 0 0/8 * * ?")
//    @Async
//    public void toDb(){
//        boolean b = lock2.tryLock();
//        if(!b){
//            log.info("任务正在执行，请稍后");
//            return;
//        }
//        log.info("开始拉取ABA数据");
//        try{
//            Integer i = dbService.synchroDataOfAba();
//            if(i!=null&&i>0){
//                sendDing("成功拉取Aba数据"+i+"条");
//                sendDing("调用同步到PG方法");
//                this.toPg();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            sendDing("拉取Aba数据失败:"+e.getMessage());
//
//        }finally {
//            lock2.unlock();
//        }
//        log.info("拉取ABA数据任务结束");
//
//    }
//
//    private void sendDing(String msg) {
//        try {
//            SendRequestParam.Text text = new SendRequestParam.Text();
//            text.setContent(msg);
//            sendDingService.sendText(text).send();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//
//
//}
