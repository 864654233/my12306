/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package cn.com.test.my12306.my12306.core;

import cn.com.test.my12306.my12306.core.proxy.ProxyUtil;
import cn.com.test.my12306.my12306.core.util.FileUtil;
import cn.com.test.my12306.my12306.core.util.JsUtil;
import cn.com.test.my12306.my12306.core.util.JsonBinder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AsyncTicketQuery {

    public JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
    private static Logger logger = LogManager.getLogger(AsyncTicketQuery.class);
    @Autowired
    private CommonUtil commonUtil;

    private BlockingQueue<Map<String,String>> queue ;
    @Autowired
    private ClientTicket ct;

    @Autowired
    ProxyUtil proxyUtil;
  /*  @Autowired
    ClientTicketA cta;*/

    @Async("myTaskAsyncPool")
    public void run(){
        /*
            代理地址：http://www.xiladaili.com/gaoni/4/
         */
        //代理设置
        HttpHost proxy = new HttpHost("119.101.116.253",9999);
//        HttpHost proxy1 = new HttpHost("106.14.162.110", 8080, "http");
//        HttpHost proxy1 = new HttpHost("110.73.5.53", 8123);
        HttpHost proxy1 = new HttpHost("118.163.120.181", 58837);




        queue = ct.queue;
        Random random = new Random();
        int max=3000;
        int min=1500;
//        max = 0;

        while(ct.canRun){
            if(commonUtil.getUseProxy()==1) {
                String proxIp = proxyUtil.getProxyIp();
                if (null != proxIp) {
                    proxy1 = new HttpHost(proxIp.split(":")[0], Integer.valueOf(proxIp.split(":")[1]));
                    logger.info("有代理{}", proxIp);
                } else {
                    proxy1 = null;
                }
            }
            //超时设置
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(3000).setConnectionRequestTimeout(20000)
//                    .setProxy(proxy1)
                    .setSocketTimeout(15000).build();
            if(commonUtil.getUseProxy()==1){
                requestConfig = RequestConfig.custom()
                        .setConnectTimeout(3000).setConnectionRequestTimeout(20000)
                        .setProxy(proxy1)
                        .setSocketTimeout(15000).build();
            }


            long startTime = System.currentTimeMillis();
            CloseableHttpClient httpClient=null;
            try{
                int a = max==0?0:random.nextInt(max)%(max-min+1) + min;
                Thread.sleep(Long.parseLong(a+"") );
                if(ct.nullCount.intValue()>100){
                    logger.info("多次查询错误，重置查询地址");
                    ct.nullCount.set(0);
                    ct.queryInit(null);
                }

                String fromCode = commonUtil.getFromCode();
                String toCode = commonUtil.getToCode();

                BasicCookieStore cookieStore = genCookie(fromCode,toCode);

                if(commonUtil.isCdnDone()){
                    httpClient = TicketHttpClient.getRetryClient(cookieStore);
                }else{
                    httpClient = TicketHttpClient.getClient(cookieStore);
                }



                String queryIp = commonUtil.getUseProxy()==1 && proxyUtil.getProxySize()>=8?"kyfw.12306.cn": commonUtil.getIp();
//            String queryIp = commonUtil.getIp();
//            ct.setLeftTicketUrl("leftTicket/queryA");

                String urlStr = "http://"+queryIp+"/otn/"+ct.getLeftTicketUrl()+"?leftTicketDTO.train_date="+ commonUtil.getBuyDate()+"&leftTicketDTO.from_station="+fromCode+"&leftTicketDTO.to_station="+toCode+"&purpose_codes=ADULT";
//            logger.info("url:{}",urlStr);
                HttpGet httpget = new HttpGet(urlStr);
                httpget.setHeader("Host", "kyfw.12306.cn");//设置host
                httpget.setHeader("If-Modified-Since", "0");
                httpget.setHeader("Cache-Control", "no-cache");
                httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
//                httpget.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpget.setConfig(requestConfig);
//                ct.httpclient
//            HttpResponse response =  ct.httpclient.execute(httpget);
                HttpResponse response =  httpClient.execute(httpget);
                HttpEntity entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                if(statusCode!=200){
                    logger.info("查询错误：{}，ip:{},statusline:{}",ct.nullCount.get(),queryIp,response.getStatusLine());
                    ct.nullCount.addAndGet(1);
                    ((CloseableHttpResponse) response).close();
                    continue ;
                }
                String content = EntityUtils.toString(entity, "UTF-8");
                Map<String, Object> rsmap = null;
                if(content.contains("DOCTYPE html PUBLIC") || content.contains("502 Bad Gateway")){
                    logger.error("网络存在问题，被禁");
                    continue;
                }
                if(content.contains("html") || content.contains("script")){
                    logger.error("查询结果异常");
                    continue;
                }
                rsmap = this.jsonBinder.fromJson(content, Map.class);
                if(null==rsmap){
                    long nullTime = System.currentTimeMillis();
                    logger.info("查票为空,用时：{}秒{}",(nullTime-startTime)/1000,(nullTime-startTime)%1000);
                    continue;
                }
                String status = rsmap.get("httpstatus")+"";
                if(status.equalsIgnoreCase("200")){
                    Map data = (Map)rsmap.get("data");
                    if(data.size()>0){
                        List<String> arr = (List<String>)data.get("result");
                        Map<String,Map<String,String>> map = new ConcurrentHashMap<String,Map<String,String>>();
                        commonUtil.jiexi(arr,map);
                        List< Map<String,String>> youpiao= commonUtil.getSecretStr(map, commonUtil.getToBuyTrains(), commonUtil.getToBuySeat());
                        if(youpiao.size()>0){
                            for(Map<String,String> map1:youpiao){
                                String chehao = map1.get("chehao");
                                String tobuySeat = map1.get("toBuySeat");
                                Long shijian = ct.getBlackMap().get(chehao+"_"+tobuySeat);
                                if(null!=shijian ){
                                    if(System.currentTimeMillis()<shijian){
                                        logger.info("发现僵尸票，暂不处理");
                                        continue;
                                    }
                                }
                                queue.put(map1);

                            }
                        }
//                       System.out.println(queryIp + "查询成功");
//                        ct.ipSet.add(queryIp);
                        //有一次查询成功，说明地址还能用
                        ct.nullCount.set(0);
                        commonUtil.setUsefulList(queryIp);
                        logger.info("==========================={}:查询成功,状态:{},随机等待{}毫秒,ipSet {}=================",queryIp,statusCode,a,ct.ipSet.size());
                        if(ct.ipSet.size()>500){
                            String ips ="";
                            for(String str:ct.ipSet){
                                ips+="\""+str+"\",";
                            }
                            FileUtil.saveAs(ips,"D:/my12306/allIP.txt");
                            System.exit(0);
                        }
                    }
                }
                ((CloseableHttpResponse) response).close();
            }catch (ConnectTimeoutException e1){
                long connTime = System.currentTimeMillis();
                logger.info("ConnectTimeout查询超时1,用时：{}秒{}",(connTime-startTime)/1000,(connTime-startTime)%1000);
//                logger.error("ConnectTimeout查询超时1");
            }catch (SocketTimeoutException se){
                logger.error("socketTimeout查询超时");
            }catch (InterruptedException ie){

            }catch (Exception e){
//                logger.error("查询出錯x",e);
                logger.error("查询出錯x",e);
            }finally {
                try{
                    httpClient.close();
                }catch (Exception e){

                }
            }

        }

    }

    public BasicCookieStore genCookie(String fromCode,String toCode){
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie _jc_save_fromStation = new BasicClientCookie("_jc_save_fromStation",JsUtil.escape(commonUtil.getStationMap().get(fromCode)+","+fromCode));
        _jc_save_fromStation.setDomain("kyfw.12306.cn");
        _jc_save_fromStation.setPath("/");

        BasicClientCookie _jc_save_toStation = new BasicClientCookie("_jc_save_toStation",JsUtil.escape(commonUtil.getStationMap().get(toCode)+","+toCode));
        _jc_save_toStation.setDomain("kyfw.12306.cn");
        _jc_save_toStation.setPath("/");

        BasicClientCookie _jc_save_fromDate = new BasicClientCookie("_jc_save_fromDate",commonUtil.getBuyDate());
        _jc_save_fromDate.setDomain("kyfw.12306.cn");
        _jc_save_fromDate.setPath("/");

        BasicClientCookie _jc_save_toDate = new BasicClientCookie("_jc_save_toDate",commonUtil.getToday());
        _jc_save_toDate.setDomain("kyfw.12306.cn");
        _jc_save_toDate.setPath("/");

        BasicClientCookie _jc_save_wfdc_flag = new BasicClientCookie("_jc_save_wfdc_flag","dc");
        _jc_save_wfdc_flag.setDomain("kyfw.12306.cn");
        _jc_save_wfdc_flag.setPath("/");

        cookieStore.addCookie(_jc_save_fromStation);
        cookieStore.addCookie(_jc_save_toStation);
        cookieStore.addCookie(_jc_save_fromDate);
        cookieStore.addCookie(_jc_save_toDate);
        cookieStore.addCookie(_jc_save_wfdc_flag);
        return cookieStore;

    }


}
