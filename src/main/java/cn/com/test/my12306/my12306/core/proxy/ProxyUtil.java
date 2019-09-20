
package cn.com.test.my12306.my12306.core.proxy;

import cn.com.test.my12306.my12306.core.ClientTicket;
import cn.com.test.my12306.my12306.core.CommonUtil;
import cn.com.test.my12306.my12306.core.TicketHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

@Component
public class ProxyUtil {


    @Autowired
    CommonUtil commonUtil;
    @Autowired
    private ClientTicket ct;

    private static Logger logger = LogManager.getLogger(ProxyUtil.class);

    public static Set<String> proxSet = new HashSet<String>();

    /**
     * 获取代理Ip并校验可用性
     * @return
     */
    public static volatile boolean canGetIp = true;
    public String getProxyIps() {
        String ipStr = "";
        if(canGetIp) {
            canGetIp = false;
            StringBuffer ipSb = new StringBuffer("");
            String baseUrl = "http://www.xiladaili.com/gaoni/";

            int i = 1;
            while (getProxySize() < 20) {
                String url = baseUrl + i + "/";
                try {
                    Document doc = Jsoup.parse(new URL(url).openStream(), "UTF-8", url);
                    System.out.println("正在加载：" + url);
                    Element table = doc.getElementsByClass("fl-table").get(0);
                    Elements trs = table.select("tr");
                    for (Element tr : trs) {
                        Elements tds = tr.select("td");
                        if (null != tds && tds.size() > 0) {
                            Element td = tds.get(0);
                            ipSb.append(td.text()).append(",");
                        }
                    }
                    ipStr = ipSb.toString();
                    ipStr = ipStr.substring(0, ipStr.length() - 1);
                    freshProxy(ipStr);
                    i++;
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    canGetIp = true;
                }
            }
            canGetIp = true;
        }
        return ipStr;
    }

    /**
     * 获取随机代理IP
     * @return
     */
//    public synchronized  String getProxyIp(){
    public  String getProxyIp(){
        String proxyIp = null;
        int i =0;
        int randomNum = new Random().nextInt(proxSet.size()>0?proxSet.size():1);
        if(null!=proxSet && proxSet.size()>0){
            for(String ip:proxSet){
                if(i==randomNum){
                    proxyIp = ip;
                }else if(i>proxSet.size()){
                    proxyIp = null;
                    break;
                }
                i++;
            }
        }
        return proxyIp;
    }

    public  int getProxySize(){
        int i =0;
        if(null!=proxSet && proxSet.size()>0){
            i=proxSet.size();
        }

        return i;
    }
    public void resetProxy(){
        proxSet.clear();
    }

    public void freshProxy(String ips){
        long allUsed = 0,used=0;
        try {

            if(StringUtils.isEmpty(ips)){
                return ;
            }
            int i=0;
            String[] s = ips.split(",");
            for(String ss:s){
                long start = System.currentTimeMillis();
                boolean flag = checkIp(ss,1);
                logger.info("第{}次校验IP，结果：{}",i++,flag);
                if(flag){
                    addProxSet(ss);
                }
                long end = System.currentTimeMillis();
                used = end-start;
                allUsed+=used;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("代理刷新完毕,用时：{}分{}秒",allUsed/1000/60,allUsed/1000%60);
    }

    public void addProxSet(String ip){
        if(null==proxSet){
            proxSet = new HashSet<String>();
        }
        proxSet.add(ip);
    }

    /**
     * 校验已有代理是否可用，不可用的剔除
     */
    public void checkProxSet(){
        if(null!=proxSet && proxSet.size()>1){
            logger.info("开始校验已有IP");
            long start = System.currentTimeMillis();
            Iterator iterator = proxSet.iterator();
            while(iterator.hasNext()){
                String ip = iterator.next().toString();
                if(!checkIp(ip,3)){
                    logger.info("移除前：{}",proxSet.size());
                    iterator.remove();
                    logger.info("移除后：{}",proxSet.size());
                }
            }
            /*for(String ip:proxSet){
                if(!checkIp(ip,3)){
                    logger.info("移除前：{}",proxSet.size());
                    proxSet.remove(ip);
                    logger.info("移除后：{}",proxSet.size());
                }
            }*/
            long end = System.currentTimeMillis();
            long used = (end-start)/1000;
            logger.info("校验已有IP结束，用时：{}分{}秒",used/60,used%60);
        }
    }

    /**
     * 校验单个IP是否可用
     * @param ip 需要校验的IP
     * @param retryCount 校验次数 默认1次
     * @return
     */
    public boolean checkIp(String ip,int retryCount) {
        boolean flag = false;
        CloseableHttpClient httpClient = null;
        long used = 0;
        for (int i = 0; i < retryCount; i++) {
            try {
                long start = System.currentTimeMillis();
                String[] x = ip.split(":");

                HttpHost proxy1 = new HttpHost(x[0], Integer.valueOf(x[1]));
                //超时设置
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(3000).setConnectionRequestTimeout(3000)
                        .setProxy(proxy1)
                        .setSocketTimeout(5000).build();
                httpClient = TicketHttpClient.getClient();
                String urlStr = "http://kyfw.12306.cn/otn/" + ct.getLeftTicketUrl() + "?leftTicketDTO.train_date=" + commonUtil.getBuyDate() + "&leftTicketDTO.from_station=" + commonUtil.getFromCode() + "&leftTicketDTO.to_station=" + commonUtil.getToCode() + "&purpose_codes=ADULT";
                HttpGet httpget = new HttpGet(urlStr);
                httpget.setHeader("Host", "kyfw.12306.cn");//设置host
                httpget.setHeader("If-Modified-Since", "0");
                httpget.setHeader("Cache-Control", "no-cache");
                httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                httpget.setConfig(requestConfig);
                HttpResponse response = null;
                try {
                    response = httpClient.execute(httpget);
                } catch (Exception e) {
                    logger.info("{}获取代理execute时出错", ip);
                    flag = false;
                    continue;
                }
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity, "UTF-8");
                System.out.println("dailidailidailidaili" + content);
                long end = System.currentTimeMillis();
                used = end - start;
//        if(!StringUtils.isEmpty(content) && content.contains("data")){
                if (!StringUtils.isEmpty(content) && content.startsWith("{\"data\"")) {
                    if (used / 1000 < 10) {//响应小于5秒的加入
//                addProxSet(ss);
                        logger.info("可用代理，用时：{}", used / 1000);
                        flag = true;
                        break;
                    }
                }
            } catch (Exception e) {
                logger.info("校验代理出错");
                flag = false;
            }finally {
                if(null!=httpClient){
                    try {
                        httpClient.close();
                    } catch (Exception e) {
//                        e.printStackTrace();
                    }
                }
            }
        }
        return flag;
    }

    public static void main(String[] args){
        ProxyUtil parser= new ProxyUtil();
        String ips= parser.getProxyIps();
        System.out.println("执行完了"+ips);
    }

}
