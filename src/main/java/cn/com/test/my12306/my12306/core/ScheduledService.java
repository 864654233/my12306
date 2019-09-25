
package cn.com.test.my12306.my12306.core;

import cn.com.test.my12306.my12306.core.proxy.ProxyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledService {

    @Autowired
    ClientTicket ct;
    @Autowired
    CommonUtil commonUtil;

    @Autowired
    ProxyUtil proxyUtil;
    private static final Logger logger = LogManager.getLogger(ScheduledService.class);

    /**
     * 晚上23点关闭任务<br/>
     * 主要关闭检票线程 主线程等待
     */
    @Scheduled(cron="0 0 23 * * ?")
//    @Scheduled(cron="0 23/5 10 * * ?")
    public void shutdownCT(){
        logger.info("服务器维护时间，关闭多线程查票");
        ct.shutdownqueryThread();
//        cta.shutdownQueryThread();
    }


    /**
     * 早上6点重新开启任务
     * cron需晚于DateUtil中的startTimeStr
     */
    @Scheduled(cron="55 59 5 * * ?")
//    @Scheduled(cron="50 25/5 10 * * ?")
    public void startCT(){
        logger.info("启动主线程开始刷票");
        try {
            if(commonUtil.getUseProxy()==1){
                proxyUtil.resetProxy();
            }
//                ct.run();
            ct.startQueryThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 早上6点到晚上23点每隔15分钟 校验下登陆状态
     *
     */
    @Scheduled(cron="0 0/15 7-22 * * ?")
    public void checkOnline(){
        ct.checkOnlineStatus2(null);
    }
    /**
     * 早上6点5分每隔15分钟 校验下登陆状态
     *
     */
    @Scheduled(cron="0 5/15 6 * * ?")
    public void checkOnline1(){
        ct.checkOnlineStatus2(null);
    }

    /**
     * 每2个小时刷新下代理
     */
   /* @Scheduled(cron="0 0 8-22/2 * * ?")
    public void freshProxy(){
        try {
            if(commonUtil.getUseProxy()==1){
                proxyUtil.getProxyIps();
             }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /**
     * 每隔一个小时校验一次已有代理是否可用
     */
   /* @Scheduled(cron="0 0 7-22/1 * * ?")
    public void checkProxy(){
        try {
            if(commonUtil.getUseProxy()==1){
                proxyUtil.checkProxSet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
