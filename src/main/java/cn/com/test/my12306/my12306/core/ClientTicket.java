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
import cn.com.test.my12306.my12306.core.util.DateUtil;
import cn.com.test.my12306.my12306.core.util.FileUtil;
import cn.com.test.my12306.my12306.core.util.ImageUtil;
import cn.com.test.my12306.my12306.core.util.JsonBinder;
import cn.com.test.my12306.my12306.core.util.mail.MailUtils;
import com.sun.net.httpserver.Headers;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A example that demonstrates how HttpClient APIs can be used to perform
 * form-based logon.
 */
@Component
public class ClientTicket /*implements ApplicationRunner*/{
    @Autowired
    CommonUtil commonUtil ;
    @Autowired
    ClientTicket ct;

    @Autowired
    ProxyUtil proxyUtil;

    @Autowired
    AsyncTicketBook asyncTicketBook;

    @Autowired
    AsyncTicketQuery asyncTicketQuery;


    @Autowired
    MailUtils mailUtils ;
    private static Logger logger = LogManager.getLogger(ClientTicket.class);
    private String rancode = "";
    BasicCookieStore cookieStore = null;
    CloseableHttpClient httpclient = null;
    RequestConfig requestConfig = null;/*RequestConfig.custom()
            .setConnectTimeout(3000).setConnectionRequestTimeout(3000)
            .setSocketTimeout(3000).build();*/

    public JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
    private String RAIL_DEVICEID ="";
    private String RAIL_EXPIRATION = "";
//    private String hosts="121.18.230.86";
    private Map<String,Integer> trainSeatMap = new ConcurrentHashMap<String,Integer>();
    private Map<String,Long> trainSeatTimeMap = new ConcurrentHashMap<String,Long>();
    public AtomicInteger nullCount = new AtomicInteger(0);
    public Set<String> ipSet = new HashSet<>();


    public String hosts="kyfw.12306.cn";
    public String exserviceHosts="exservice.12306.cn";
//    public String hosts="112.90.135.94";
    private String leftTicketUrl = "leftTicket/query";
    ScheduledExecutorService es = null;
    //是否可以运行查票 用于定时任务
   volatile boolean canRun;
    Boolean isReady = false;//用于查看主线程是否运行完毕

    public BlockingQueue<Map<String,String>> queue = new LinkedBlockingQueue<Map<String, String>>(10);

    public ClientTicket(BasicCookieStore cookieStore, CloseableHttpClient httpclient) {
        this.cookieStore = cookieStore;
        this.httpclient = httpclient;
        this.hosts = commonUtil.getIp();
        rancode = "";
    }

    public ClientTicket(BasicCookieStore cookieStore) {
        this.cookieStore = cookieStore;
        this.hosts = commonUtil.getIp();
        rancode = "";
    }

    public ClientTicket() {
        cookieStore = new BasicCookieStore();
         requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000).setConnectionRequestTimeout(30000)
                .setSocketTimeout(30000).build();

        SSLContext sslContext = null;
            try{
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        }catch (Exception e){
                e.printStackTrace();
            }
        httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .build();
        rancode = "";
    }

    public CloseableHttpClient getHttpclient() {
        return this.httpclient;
    }

    public void setHttpclient(CloseableHttpClient httpclient) {
        this.httpclient = httpclient;

    }

    @PreDestroy
    public void testDestroy(){
        logger.info("exit 哈哈");
    }

    public Map<String, Integer> getTrainSeatMap() {
        return trainSeatMap;
    }

    public void setTrainSeatMap(Map<String, Integer> trainSeatMap) {
        this.trainSeatMap = trainSeatMap;
    }

    public Map<String, Long> getTrainSeatTimeMap() {
        return trainSeatTimeMap;
    }

    public void setTrainSeatTimeMap(Map<String, Long> trainSeatTimeMap) {
        this.trainSeatTimeMap = trainSeatTimeMap;
    }

    public String getLeftTicketUrl() {
        return leftTicketUrl;
    }

    public void setLeftTicketUrl(String leftTicketUrl) {
        this.leftTicketUrl = leftTicketUrl;
    }

    public static void getAllHeaders(CloseableHttpResponse response){
        Header[] headers = response.getAllHeaders();
        System.out.println("开始获取header");
        for(Header h:headers){
            System.out.println(h.getName()+":"+h.getValue());
        }
        System.out.println("结束获取header");
    }

    public  void getAllCookies(BasicCookieStore cookieStore){
        System.out.println("开始获取cookie");
        List<Cookie> cookies = cookieStore.getCookies();
        if (cookies.isEmpty()) {
            System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
        System.out.println("结束获取cookie");
    }

    /**
     * 将cookie写入到文件
     */
    public  void writeCookies2File(){
        List<Cookie> cookies = cookieStore.getCookies();
        String c="";
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
                Cookie ck= cookies.get(i);
               c+=ck.getName()+","+ck.getValue()+","+ck.getDomain()+","+ck.getPath()+";";
            }
            if(c.endsWith(";")) c=c.substring(0,c.length()-1);
        FileUtil.saveAs(c,commonUtil.sessionPath+commonUtil.getUserName()+"_12306Session.txt");
        System.out.println("结束获取cookie");
    }

    public  void resetCookiesFile(){
        FileUtil.saveAs("",commonUtil.sessionPath+commonUtil.getUserName()+"_12306Session.txt");
    }

    /**
     * 将cookie读取到cookiestore 查看是否可以用
     */
    public void readFile2Cookie(){
        String context = FileUtil.readByLines(commonUtil.getSessionPath()+commonUtil.getUserName()+"_12306Session.txt");

        if(null!=context && !"".equals(context)){
            String[] aCookie = context.split(";");
            for(String ck:aCookie){
                String[] bCookie = ck.split(",");
                BasicClientCookie acookie = new BasicClientCookie(bCookie[0], bCookie[1]);
                acookie.setDomain(bCookie[2]);
                acookie.setPath(bCookie[3]);
                cookieStore.addCookie(acookie);

            }
        }

    }


    /**
     *
     * @param headers
     */
    public void shuapiao(Header[] headers){
        if(null==headers || headers.length==0){
            headers = new Header[3];
            headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            headers[1] = new BasicHeader("Host","kyfw.12306.cn");
        }
        headers = new BasicHeader[7];
        headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
        headers[1] = new BasicHeader("Host","kyfw.12306.cn");
        headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
        headers[3] = new BasicHeader("Accept","*/*");
        headers[4] = new BasicHeader("Accept-Encoding","gzip, deflate");
        headers[5] = new BasicHeader("Accept-Language","zh-Hans-CN,zh-Hans;q=0.8,en-US;q=0.5,en;q=0.3");
        headers[6] = new BasicHeader("Content-Type","application/x-www-form-urlencoded");
        while(true) {
            try {
                headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/leftTicket/init");
                //https://kyfw.12306.cn/otn/leftTicket/queryZ?leftTicketDTO.train_date=2018-02-12&leftTicketDTO.from_station=SJP&leftTicketDTO.to_station=BJP&purpose_codes=ADULT

                HttpUriRequest shuapiao = RequestBuilder.get()
                        .setUri("https://" + hosts + "/otn/"+leftTicketUrl+"?leftTicketDTO.train_date=2018-02-25&leftTicketDTO.from_station=SJP&leftTicketDTO.to_station=BJP&purpose_codes=ADULT")
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                        .build();
                CloseableHttpResponse response = httpclient.execute(shuapiao);
                getAllCookies(this.cookieStore);
                HttpEntity entity = response.getEntity();
                Map<String, Object> rsmap = null;
                rsmap = this.jsonBinder.fromJson(EntityUtils.toString(entity), Map.class);
//                System.out.println(rsmap.size() + " " + rsmap);
                String status = rsmap.get("httpstatus") + "";
                if (status.equalsIgnoreCase("200")) {
                    Map data = (Map) rsmap.get("data");
                    if (data.size() > 0) {
                        List<String> arr = (List<String>) data.get("result");
                        System.out.println(arr.size() + "a " + arr.get(0));
                        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
                        commonUtil.jiexi(arr, map);
                        List< Map<String,String>> youpiao= commonUtil.getSecretStr(map, commonUtil.getTrains(), commonUtil.getSeats());
                        if(youpiao.size()>0){
                            for(Map<String,String> map1:youpiao){
                                queue.put(map1);
                            }
                         /*  Map<Integer,String> mapQueue=new HashMap<Integer,String>();
                           mapQueue.put(0,secretStr.split(",")[0]);//secret
                           mapQueue.put(1,secretStr.split(",")[1]);//席别
                           mapQueue.put(2,secretStr.split(",")[2]);//车次
                           System.out.println();
                           queue.put(mapQueue);*/
                        }

                    }
                }
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("刷票时出错");
                e.printStackTrace();
            }

        }
    }


    public void shutdownqueryThread(){
        ct.canRun =false;
//        es.shutdownNow();
    }
    public void startQueryThread(){
        ct.canRun = true;
        for(int i =0;i<10;i++){
            asyncTicketQuery.run();
        }
    }

    public void setCookieStore(CloseableHttpResponse httpResponse) {

        Header[] headers = httpResponse.getHeaders("Set-Cookie");
        if(null==headers || headers.length==0){
            return ;
        }
        System.out.println("----setCookieStore");
        for(Header header:headers){
            String setCookies = header.getValue();
            String[] cookieArr = setCookies.split(";");
            String key = cookieArr[0].split("=")[0];
            String val = cookieArr[0].split("=")[1];
            BasicClientCookie cookie = new BasicClientCookie(key,val);
            if(key.equals("JSESSIONID")){
                //删除已有的JSESSIONID cookie
                List<Cookie> cookieList = cookieStore.getCookies();
                Iterator<Cookie> iterator = cookieList.iterator();
                while ( iterator.hasNext() ) {
                    Cookie ck = iterator.next();
                    if(ck.getName().equals("JSESSIONID")){
                        iterator.remove();
                    }
                }
                Cookie[] cks = new Cookie[cookieList.size()];
                cookieStore.clear();
                cookieStore.addCookies(cookieList.toArray(cks));
            }
            for(int i =1;i<cookieArr.length;i++){
                String key1 = cookieArr[i].split("=")[0];
                String val1 = cookieArr[i].split("=")[1];
                if(key1.trim().equalsIgnoreCase("path")){
                    cookie.setPath(val1);
                }
                if(key1.trim().equalsIgnoreCase("domain")){
                    cookie.setDomain(val1);
                }
            }
            if(null==cookie.getDomain()){
                cookie.setDomain("kyfw.12306.cn");
            }

        cookieStore.addCookie(cookie);
        }
        System.out.println("----setCookie Done-----");
    }




    /**
     * 获取验证码弹窗
     * @param url
     * @return
     * @throws IOException
     */
    public String getCode(String url,Header[] headers) throws IOException {
        //JFrame frame = new JFrame("验证码");
        this.rancode="";
        String codeName = System.currentTimeMillis()+".jpg";
        String imagePath = "";
        String imageBase64="";
        int valiCount = 1;
        while(StringUtils.isEmpty(imagePath)){
            if(valiCount>1){
                try {
                    logger.info("{}次获取验证码",valiCount);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Map<String,String> rsMap = getCodeByte(url, headers,codeName);
            imagePath = rsMap.get("imagePath");
            imageBase64 = rsMap.get("imageBase64");
            valiCount++;
        }
        if(CommonUtil.autoCode.equals("1")){
            String rs=ImageUtil.shibie360(imageBase64);
//            String rsCode = ImageUtil.getZuobiao(rs);
            this.rancode=rs;
        }else {
//        new TipTest("","","请输入验证码");
//            JLabel label = new JLabel(new ImageIcon(getCodeByte(url, headers,codeName)),
//                    JLabel.CENTER);
            JLabel label = new JLabel(new ImageIcon(imagePath),
                    JLabel.CENTER);

            label.setBounds(0, 0, 295, 220);
            label.setVerticalAlignment(SwingConstants.TOP);
            label.addMouseListener(new RecordListener());

            JOptionPane.showConfirmDialog(null, label,
                    "请输入验证码", JOptionPane.DEFAULT_OPTION);

        }
//        String fileName = System.currentTimeMillis()+".jpg";
//        ImageUtil.download("https://aa/passport/captcha/captcha-image?login_site=E&module=login&rand=sjrand&"+Math.random(),fileName,"");
        return this.rancode;
    }


    /**
     * 获取指定url的验证码图片字节信息
     * @param url
     * @return
     */

    public  Map<String,String> getCodeByte(String url,Header[] headers,String codeName) {
        HttpGet get = new HttpGet(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in=null;
        OutputStream out = null;
        byte[] bytse = null;
        Map<String,String> rsMap = new HashMap<String,String>();
        String fileName="";
        CloseableHttpResponse response = null;
        try {
                HttpGet hget = new HttpGet("https://"+hosts+"/passport/captcha/captcha-image64?login_site=E&module=login&rand=sjrand&" + Math.random());
               for(Header h:headers){
                   hget.addHeader(h);
               }
//                CloseableHttpResponse response = httpclient.execute(new HttpGet("https://"+hosts+"/passport/captcha/captcha-image?login_site=E&module=login&rand=sjrand&" + Math.random()));
                response = httpclient.execute(hget);

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity, "UTF-8");
                Map<String, Object> rsmap = null;
                rsmap = this.jsonBinder.fromJson(content, Map.class);
                String code = rsmap.get("result_code")+"";
                String image =  rsmap.get("image")+"";
                if(null!=image){
                    String savePath=CommonUtil.sessionPath+CommonUtil.codePath;
                    File sf=new File(savePath);
                    if(!sf.exists()){
                        sf.mkdirs();
                    }
                    ImageUtil.GenerateImage(image,savePath+File.separator+codeName);
                    fileName = savePath+File.separator+codeName;
                    rsMap.put("imagePath",fileName);
                    rsMap.put("imageBase64",image);
                }else{
                    fileName="";
                }

               /* bytse=  EntityUtils.toByteArray(entity);
                //保存图片到本地
                in = entity.getContent();
                File sf=new File(CommonUtil.sessionPath+File.separator+CommonUtil.codePath);
                if(!sf.exists()){
                    sf.mkdirs();
                }
                out = new FileOutputStream(new File(CommonUtil.sessionPath+File.separator+CommonUtil.codePath+File.separatorChar+codeName));

                out.write(bytse,0,bytse.length);
                out.flush();
                in.close();


                EntityUtils.consume(entity); //Consume response content*/
            }catch(Exception e ){
                rsMap.put("imagePath","");
               e.printStackTrace();
            }finally {
            closeResponse(response);
        }
            return rsMap;

    }

    public void closeResponse(CloseableHttpResponse response) {
        try {
            if (null != response) {
                response.close();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    //    public void run(ApplicationArguments var1) throws Exception {
    public void run() throws Exception {
        if(commonUtil.getUseProxy()==1){
         new Thread( () -> proxyUtil.getProxyIps() ).start();
//            proxyUtil.getProxyIps();
        }
        canRun = false;
        //是否是正常的预定时间
        if(DateUtil.isNormalTime()){
            canRun =true;
        }else{
            logger.info("维护时间，暂停查询");
            return ;
        }

        Header[] headers = new BasicHeader[3];
        headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
        headers[1] = new BasicHeader("Host","kyfw.12306.cn");
        headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");

        try {
            //初始化页面


            //登陆
            boolean loginFlag=true;
//            ct.setHosts();
////            while( ct.login(headers)){
            while( loginFlag){
                loginFlag= !ct.login(headers);
                if(!loginFlag){
                    logger.info("登陆成功，继续执行下面代码");
                    break;
                }
            }

            //刷票
            // ct.shuapiao(headers,"","");
           /* new  Thread(new Runnable(){
                public void run(){
                    System.out.println("主线程刷票");
                    ct.shuapiao(headers);
                }
            }).start();*/
            ct.queryInit(headers);//设置查询地址 queryA queryZ
            ct.getAllCookies(ct.cookieStore);
//            ct.getCodeByte(headers);
            AddCaptchaCookie();
            for(int i =0;i<10;i++){
                asyncTicketQuery.run();
            }
//            ct.xianchengshuapiao(es);
            //订票线程？主程序订票
            if(commonUtil.getAutoSub().equals("1")){
//                new Thread(new AutoTicketBook(ct,queue,ct.getHttpclient(),headers,cookieStore)).start();
                for(int i = 0;i<10;i++){
                    asyncTicketBook.run();
                }
            }else{
                new Thread(new TicketBook(ct,queue,ct.getHttpclient(),headers,cookieStore)).start();
            }
           /* while(ct.queue.size()==0){
                Thread.sleep(200);
            }
            System.out.println("有票了 开始预订");
            es.shutdownNow();*/

            //不需要停止
            //es.shutdown();
            //检查是否还在线
            //验证登陆状态
           /* Map<String,Object> onlineMap = this.checkOnlineStatus(headers);
            if(null!=onlineMap && onlineMap.size()>0){
                if("0".equalsIgnoreCase(onlineMap.get("result_code")+"")){
                    System.out.println("验证时候已经登陆");
                }else{
                    System.out.println("验证时候未登录");
                    boolean loginFlag1=true;
//            while( ct.login(headers)){
                    while( loginFlag1){
                        loginFlag1= !ct.login(headers);
                        if(!loginFlag1){
                            System.out.println("登陆成功，开始预定车票");
                            break;
                        }

                    }
                }
            }*/


            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
          /*  while(true){
                Thread.sleep(1000);
            }*/

        }catch (Exception e){
            e.printStackTrace();
        }finally {
//            httpclient.close();
        }
    }

    public void AddCaptchaCookie() {
        BasicClientCookie acookie = new BasicClientCookie("current_captcha_type", "Z");
//        acookie.setDomain("kyfw.12306.cn");
        acookie.setDomain(hosts);
        acookie.setPath("/");
        cookieStore.addCookie(acookie);
        ct.getAllCookies(ct.cookieStore);
    }

    /**
     *	鼠标点击验证码，记录坐标
     *
     * @author xiaoQ
     * @since 2011-12-21
     * @version 1.0
     */
    class RecordListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            //refreshRandCode();
            int x=e.getX();
            int y=e.getY();
            rancode+=rancode.equals("")?x+","+(y-30):","+x+","+(y-30);
            System.out.println(x+","+y+"  rancode:"+rancode);
        }
    }

    public void resetRancode(){
        this.rancode = "";
    }

    public Map<String,Object> conf(Header[] headers){
//        headers[1] = new BasicHeader("Host","kyfw.12306.cn");
//        headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
        Map<String,Object> confMap = new HashMap<String,Object>();
//        Header contentType = new BasicHeader("Host","kyfw.12306.cn");
        Header contentLen = new BasicHeader("Content-Length","0");
        CloseableHttpResponse responseConf = null;
        try {
            String url = headers[1].getValue().equalsIgnoreCase("www.12306.cn")?"/index/otn/login/conf":"/otn/login/conf";
            logger.info("header2:{}",headers[2].getValue());
            HttpUriRequest conf = RequestBuilder.post()
                    .setUri(new URI("https://"+this.hosts+url))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2])
                    .addHeader(contentLen)
                    .build();
            responseConf = httpclient.execute(conf);
            setCookieStore(responseConf);
            HttpEntity entity = responseConf.getEntity();
            String jsonStr= EntityUtils.toString(entity);
            logger.info("conf entity:{}",jsonStr);
            confMap = jsonBinder.fromJson(jsonStr,Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeResponse(responseConf);
        }
        return confMap;
    }
    public Map<String,Object> uamtkStatic(Header[] headers){
        headers[1] = new BasicHeader("Host","kyfw.12306.cn");
        headers[2] = new BasicHeader("Referer"," https://www.12306.cn/index/");
        Header headerOrigin = new BasicHeader("Origin","https://www.12306.cn");
        Map<String,Object> uamtkStaticMap = new HashMap<String,Object>();
        CloseableHttpResponse uamtkResponse = null;
        try {
            HttpUriRequest uamtkStatic = RequestBuilder.post()
                    .setUri(new URI("https://"+this.hosts+"/passport/web/auth/uamtk-static"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2])
                    .addParameter("appid","otn")
                    .build();
            uamtkResponse = httpclient.execute(uamtkStatic);
            HttpEntity entity = uamtkResponse.getEntity();
            String jsonStr= EntityUtils.toString(entity);
            logger.info("uamtkStatic entity:{}",jsonStr);
            uamtkStaticMap = jsonBinder.fromJson(jsonStr,Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeResponse(uamtkResponse);
        }
        return uamtkStaticMap;
    }

    public boolean login(Header[] headers) {
        try {
            if (null == headers || headers.length == 0) {
                headers = new BasicHeader[3];
                headers[0] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
                headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/resources/login.html");
            }
            headers[1] = new BasicHeader("Host", "www.12306.cn");
            headers[2] = new BasicHeader("Referer", "https://www.12306.cn/index/index.html");
            getIndex(headers);
//            conf(headers);
            //设置多余的cookie
            headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
            ct.getDeviceCookie(headers);
            logger.info("init page");
            headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/resources/login.html");
            HttpUriRequest initPage1 = RequestBuilder.get()//.post()
                    .setUri(new URI("https://" + this.hosts + "/otn/resources/login.html"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2])
                    .build();
            CloseableHttpResponse response3 = httpclient.execute(initPage1);
            try {
                HttpEntity entity = response3.getEntity();

                EntityUtils.consume(entity);
            } finally {
                response3.close();
            }
            headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
            headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/resources/login.html");


            this.getAllCookies(this.cookieStore);
        /*    logger.info("page inited and start to readFile2Cookie ");
            //设置一遍cookie
            readFile2Cookie();
            logger.info("check online status");*/
            //验证登陆状态
            Map<String, Object> confMap = this.conf(headers);
            logger.info("conf result:{}", confMap);
            getLogInBanner(headers);
            Map<String, Object> onlineMap = this.uamtkStatic(headers);//this.checkOnlineStatus(headers);
            if (null != onlineMap && onlineMap.size() > 0) {
                if ("0".equalsIgnoreCase(onlineMap.get("result_code") + "")) {
                    logger.info("验证时候已经登陆");
                    return true;
                } else {
                    logger.info("验证时候未登录");
                }
            }
            CloseableHttpResponse response = null;
            if (commonUtil.getLogonType().equals("1")) {
                boolean flag = loginQr(headers);
                //防止出现越界
                headers = new BasicHeader[7];
                headers[0] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
                headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/resources/login.html");
                //headers[3] = new BasicHeader("Accept","application/json, text/javascript, */*; q=0.01");
                headers[3] = new BasicHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                headers[4] = new BasicHeader("Accept-Encoding", "gzip, deflate, br");
                headers[5] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
                headers[6] = new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            } else {
                boolean checkedCode = false;
                String valicode = "";
                while (!checkedCode) {

                    //获取验证码
                    valicode = "";
                    int valiCount = 1;
                    while (valicode.equals("")) {
                   /* if(valiCount>1){
                        Thread.sleep(2000);
                    }*/
                        valicode = this.getCode("", headers);
                        valiCount++;
                    }

                    System.out.println("验证码：" + valicode);

                    logger.info("start check code");
                    //校验验证码
                    HttpUriRequest checkCode = RequestBuilder.post()
                            .setUri(new URI("https://" + this.hosts + "/passport/captcha/captcha-check"))
                            .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2])
                            .addParameter("answer", valicode)
                            .addParameter("login_site", "E")
                            .addParameter("rand", "sjrand")
                            .addParameter("_", "" + (new Date()).getTime())
                            .build();
                    response = httpclient.execute(checkCode);
                    this.getAllCookies(this.cookieStore);

                    Map<String, String> rsmap = null;
                    try {
                        HttpEntity entity = response.getEntity();
                        rsmap = this.jsonBinder.fromJson(EntityUtils.toString(entity), Map.class);
//                System.out.println("校验：" + response.getStatusLine().getStatusCode() + " " + entity.getContent() + " abc " + EntityUtils.toString(entity));
                        if (null == rsmap) {
                            System.out.println("验证码校验没有通过111");
                        } else if (rsmap.get("result_code").equalsIgnoreCase("4")) {
                            logger.info("验证码校验通过");
                            checkedCode = true;
                            break;
                        } else {
                            logger.info("验证码校验没有通过1");
                        }
                    } catch (Exception e) {
                        logger.info("验证码校验没有通过2");
                        e.printStackTrace();
                    } finally {
                        response.close();
                    }
                    logger.info("2秒后重新拉取验证码");
                    Thread.sleep(2000);
                }


                //登陆
                Map<String, String> map = new HashMap<String, String>();

                try {
                    logger.info("start login");
//                valicode = "127,30";
//                Thread.sleep(400);
                    headers = new BasicHeader[7];
                    headers[0] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                    headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
                    headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/resources/login.html");
                    //headers[3] = new BasicHeader("Accept","application/json, text/javascript, */*; q=0.01");
                    headers[3] = new BasicHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                    headers[4] = new BasicHeader("Accept-Encoding", "gzip, deflate, br");
                    headers[5] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
                    headers[6] = new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    Header headerConn = new BasicHeader("Connection", "keep-alive");
                    HttpUriRequest login = RequestBuilder.post()
                            .setUri("https://" + this.hosts + "/passport/web/login")
                            //.setUri(new URI("https://kyfw.12306.cn/passport/web/login"))
                            .addHeader(headers[0]).addHeader(headers[1]).
                                    addHeader(headers[2]).
                                    addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
//                        .addHeader(headerOrigin)
                            .addHeader(headerConn)
//                        .addHeader(headerLength)
                            .addParameter("username", commonUtil.getUserName())
                            .addParameter("password", commonUtil.getUserPwd())
                            .addParameter("appid", "otn")
                            .addParameter("answer", URLEncoder.encode(valicode, "utf-8"))
//                        .addParameter("answer", valicode)
                            .build();
                    login.addHeader("X-Requested-With", "XMLHttpRequest");
//                login.addHeader("Connection","keep-alive");
                    this.getAllCookies(this.cookieStore);
                    logger.info(login.getFirstHeader("Content-Length"));
//                Thread.sleep(400);
                    response = httpclient.execute(login);
                    logger.info("login status:{}", response.getStatusLine().getStatusCode());
                    HttpEntity entity = response.getEntity();
                    Map<String, String> rsmap = this.jsonBinder.fromJson(EntityUtils.toString(entity), Map.class);
                    if (null != rsmap && rsmap.size() > 0) {
                        String code = String.valueOf(rsmap.get("result_code")) + "";
                        if (code.equalsIgnoreCase("0")) {
                            logger.info("登陆成功");

                        } else {
                            System.out.println("登陆失败");
                            return false;
                        }
                    } else {
                        System.out.println("登陆失败，发生了302，被禁了？");
                        return false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("登陆时发生错误");
                    return false;
                } finally {
                    closeResponse(response);
                }
            }
            logger.info("check online status again");
            //再次校验登陆状态
            onlineMap = this.checkOnlineStatus(headers);
            String tk = "";
            if (null != onlineMap && onlineMap.size() > 0) {
                if ("0".equals(onlineMap.get("result_code") + "")) {
                    logger.info("再次验证时候已经登陆");
                    tk = onlineMap.get("newapptk").toString();

                } else {
                    System.out.println("再次验证时候未登录,重新登录");
                    return false;
                }
            }


            logger.info("start check client and get tk");
            //校验客户端
            try {
                headers[1] = new BasicHeader("Host", "kyfw.12306.cn");
                Header headerReffer = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/passport?redirect=/otn/login/userLogin");
                HttpUriRequest uamauthclient = RequestBuilder.post()
                        .setUri("https://" + this.hosts + "/otn/uamauthclient")
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                        .addHeader(headerReffer)
                        .addParameter("tk", tk)
                        .build();
                uamauthclient.addHeader("X-Requested-With", "XMLHttpRequest");
                uamauthclient.addHeader("Connection", "keep-alive");
                response = httpclient.execute(uamauthclient);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();


                    String jsonStr = EntityUtils.toString(entity);
                    System.out.println("entity " + jsonStr);
                    Map<String, String> map = this.jsonBinder.fromJson(jsonStr, Map.class);
                    logger.info("校验通过：{}", map.get("username"));
                    logger.info("cookie中是否有tk参数");
                    this.getAllCookies(this.cookieStore);
                    logger.info("tk cookie获取结束");
//                closeResponse(response);
                    //校验
                /*HttpUriRequest userLogin = RequestBuilder.get()
                        .setUri("https://" + this.hosts + "/otn/login/userLogin")
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                        .build();
                userLogin.addHeader("X-Requested-With", "XMLHttpRequest");
                userLogin.addHeader("Connection", "keep-alive");
                response = httpclient.execute(userLogin);
                statusCode = response.getStatusLine().getStatusCode();
                logger.info("userLogin statusCode:{}", statusCode);
                EntityUtils.consume(response.getEntity());
//                closeResponse(response);
                //跳转到 用户页面

                HttpUriRequest initMy12306 = RequestBuilder.get()
                        .setUri("https://" + this.hosts + "/otn/index/initMy12306")
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                        .build();
                initMy12306.addHeader("X-Requested-With", "XMLHttpRequest");
                initMy12306.addHeader("Connection", "keep-alive");
                response = httpclient.execute(initMy12306);
                statusCode = response.getStatusLine().getStatusCode();
                logger.info("initMy12306 statusCode:{}", statusCode);
                EntityUtils.consume(response.getEntity());*/

                    logger.info("用户登录成功");
                    //将成功的cookie写入文件
                    writeCookies2File();
                    isReady = true;
                    return true;

                }
            } finally {
                closeResponse(response);
            }

        } catch (Exception e) {
            logger.error("登陆时发生错误", e);
        }
        return false;
    }

    private void getIndex(Header[] headers){

        CloseableHttpResponse response2 = null;
        try {
            Header header = new BasicHeader("Host","www.12306.cn");
            HttpUriRequest getindex = RequestBuilder.get()
                    .setUri("https://www.12306.cn/index/index.html")
                    .addHeader(header).addHeader(headers[0])
                    .build();
            response2 = httpclient.execute(getindex);
            HttpEntity entity = response2.getEntity();
            EntityUtils.consume(entity);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null!=response2){
                try{
                    response2.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void getLogInBanner(Header[] headers){

        CloseableHttpResponse response2 = null;
        try {
            HttpUriRequest loginBanner = RequestBuilder.get()
                    .setUri("https://"+this.hosts+"/otn/index12306/getLoginBanner")
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2])
                    .build();
            response2 = httpclient.execute(loginBanner);
            setCookieStore(response2);
            HttpEntity entity = response2.getEntity();
            String jsonStr = EntityUtils.toString(entity);
            System.out.println("entity " + jsonStr);
            Map<String,String> map = this.jsonBinder.fromJson(jsonStr, Map.class);
            logger.info("getLogInBanner result:{}",map);
//            EntityUtils.consume(entity);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null!=response2){
                try{
                    response2.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 增加另外两个cookie
     * @param headers
     */
    private void getDeviceCookie(Header[] headers){

        CloseableHttpResponse response2 = null;//httpclient.execute(getDevice);
        try {
            logger.info("用到的header：{}",headers[0].getValue());
            Header headerHost = new BasicHeader("Host","kyfw.12306.cn");
            String params = "algID=stlPYD4gpV&hashCode=pAsqygwgJFux1ohtGryn1E84twryYmobdaQj6QLm4Ss&FMQw=1&q4f3=zh-CN&VySQ=FGGELzCLkZ3be5N_q7iX4OwsdRYoVSq0&VPIf=1&custID=133&VEek=unspecified&dzuS=0&yD16=0&EOQP=13c246fe6c83ce181f4fd5e79c60a4ff&lEnu=168107667&jp76=d41d8cd98f00b204e9800998ecf8427e&hAqN=Win32&platform=WEB&ks0Q=d41d8cd98f00b204e9800998ecf8427e&TeRS=728x1366&tOHY=24xx768x1366&Fvje=i1l1s1&q5aJ=-8&wNLf=99115dfb07133750ba677d055874de87&0aew=%s&E3gR=500781a8a6be6202627e398a65b7e48e&timestamp=%s";
            params = String.format(params,URLEncoder.encode(headers[0].getValue(),"utf-8"),(new Date()).getTime() );
            logger.info("获取设备信息："+params);
            HttpUriRequest getDevice = RequestBuilder.get()//.post()
                    .setUri("https://"+hosts+"/otn/HttpZF/logdevice?"+ params)
                    .addHeader(headerHost).addHeader(headers[1]).addHeader(headers[2])
                    .build();
            response2 = httpclient.execute(getDevice);
            HttpEntity entity = response2.getEntity();
            getAllHeaders(response2);

            Calendar calendar1 = Calendar.getInstance();
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
            calendar1.add(Calendar.DATE, 1000);
            calendar1.add(Calendar.HOUR,-8);
            System.out.println("getDeviceCookie get: " + response2.getStatusLine());
            String jsonStr= EntityUtils.toString(entity);
            jsonStr=jsonStr.replace("callbackFunction('","").replace("')","");
            System.out.println("entity " +jsonStr);
            Map<String,String> map = new HashMap<>();
            map = jsonBinder.fromJson(jsonStr,Map.class);
            System.out.println("Login form get: " +map);
            RAIL_DEVICEID = map.get("dfp");
            BasicClientCookie acookie = new BasicClientCookie("RAIL_DEVICEID", map.get("dfp"));
//            acookie.setDomain(".12306.cn");
            acookie.setDomain("kyfw.12306.cn");
            acookie.setPath("/");
//            acookie.setExpiryDate( calendar1.getTime());
            cookieStore.addCookie(acookie);
            BasicClientCookie bcookie = new BasicClientCookie("RAIL_EXPIRATION", map.get("exp"));
            RAIL_EXPIRATION =  map.get("exp");
//            bcookie.setDomain(".12306.cn");
            bcookie.setDomain("kyfw.12306.cn");
            bcookie.setPath("/");
//            bcookie.setExpiryDate(calendar1.getTime());
            cookieStore.addCookie(bcookie);

//            getAllCookies(cookieStore);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null!=response2){
                try{
                    response2.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public Map<String,Object> checkOnlineStatus(Header[] headers){
        Map<String,Object> map = new HashMap<String,Object>();
        if(null== headers){
            headers = new Header[3];
            headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            headers[1] = new BasicHeader("Host","kyfw.12306.cn");
            headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
        }
//        Header headerOrigin = new BasicHeader("Origin","https://exservice.12306.cn");
        Header headerReffer = new BasicHeader("Referer","https://kyfw.12306.cn/otn/passport?redirect=/otn/login/userLogin");
        HttpUriRequest getDevice = RequestBuilder.post()
                .setUri("https://"+hosts+"/passport/web/auth/uamtk")
                .addHeader(headers[0]).addHeader(headers[1])
                .addHeader(headerReffer)
//                .addHeader(headerOrigin)
//                .addParameter("appid","otn")
                .addParameter("appid","otn")
                .setConfig(requestConfig)
                .build();
       /* for(Header h:headers){
            getDevice.addHeader(h);
        }*/
       Header[] h= getDevice.getAllHeaders();
        for(Header h1:h){
            System.out.println("fffffffffffffffff"+h1);
        }
        CloseableHttpResponse response2 = null;//httpclient.execute(getDevice);
        try {
            logger.info("===================start check ");
            ct.getAllCookies(ct.cookieStore);
            response2 = httpclient.execute(getDevice);
            logger.info("=================== check  ended ");
            HttpEntity entity = response2.getEntity();


            String jsonStr= EntityUtils.toString(entity);
            logger.info("checkOnlineStatus entity:{}",jsonStr);
            map = jsonBinder.fromJson(jsonStr,Map.class);

        }catch (Exception e){
            logger.error("判断在线状态时出错",e);
//            e.printStackTrace();
        }finally {
            if(null!=response2){
                try{
                    response2.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        return map;
    }

    public void sendSuccessMail(String msg){
        mailUtils.send(msg);
    }

    public synchronized String queryInit(Header[] headers){
            CloseableHttpResponse response=null;
            String queryUrl ="";
            String responseBody = "";
            try{
                if(null== headers || headers.length<7){
                    headers = new BasicHeader[7];
                    headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                    headers[1] = new BasicHeader("Host","kyfw.12306.cn");
                    headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
                    headers[3] = new BasicHeader("Accept","*/*");
                    headers[4] = new BasicHeader("Accept-Encoding","gzip, deflate");
                    headers[5] = new BasicHeader("Accept-Language","zh-Hans-CN,zh-Hans;q=0.8,en-US;q=0.5,en;q=0.3");
                    headers[6] = new BasicHeader("Content-Type","application/x-www-form-urlencoded");
                }
                headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
                HttpUriRequest confirm = RequestBuilder.get()
//                        .setUri(new URI("https://kyfw.12306.cn/otn/leftTicket/init"))
                        .setUri(new URI("https://"+hosts+"/otn/leftTicket/init"))
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5])
//                        .addParameter("_json_att", "")
                        .addParameter("linktypeid", "dc")
                        .build();
                response = httpclient.execute(confirm);



                if(response.getStatusLine().getStatusCode()==200){
                    HttpEntity entity = response.getEntity();
                    responseBody =EntityUtils.toString(entity);
                    logger.info("查询页面初始化成功");
                    Pattern p=Pattern.compile("CLeftTicketUrl \\= '(.*?)';");
                    Matcher m=p.matcher(responseBody);
                    while(m.find()){
                        queryUrl=m.group(1);
                        setLeftTicketUrl(queryUrl);
                        logger.info("查询的地址是：{}",queryUrl);
                    }
                    headers[2]= new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");
                }else{
                    logger.warn("查询页面初始化失败 status错误:",response.getStatusLine().getStatusCode());
                }

            }catch (Exception e){
                logger.error("查询页面初始化失败:{}",responseBody,e);
            }finally {
                try{
                    response.close();
                }catch (Exception e){

                }
            }
            return queryUrl;

    }


    public void resetCookieStore(){
//        cookieStore = new BasicCookieStore();
        cookieStore.clear();
//        httpclient = HttpClients.custom()
//                .setDefaultCookieStore(cookieStore)
//                .build();
//        ct.cookieStore = cookieStore;
//        ct.httpclient = httpclient;
    }

    public boolean loginQr(Header[] headers){
        boolean flag =false;
        Map<String,Object> qrMap = createQr(headers);

        if(qrMap.size()>0){
            //获取登录二维码
            String status =  StringUtils.isEmpty(qrMap.get("result_code"))?"":String.valueOf(qrMap.get("result_code"));
            if(status.equals("0")){
                String image = StringUtils.isEmpty(qrMap.get("image"))?"":String.valueOf(qrMap.get("image"));
                String uuid = StringUtils.isEmpty(qrMap.get("uuid"))?"":String.valueOf(qrMap.get("uuid"));
                //生成二维码弹窗
                JDialog dialog = new JDialog();
                ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
                for (int i = 0; i < 1; i++) {
                    singleThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                logger.info("8888888888888888888888888888");
                                genereteQr(dialog,image);
                                logger.info("99999999999999999999999999999999");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                        //校验二维码是否已登录
                String  result_code = "";
                String uamtk = "";
                while(!result_code.equals("2")){
                    Map<String,Object> checkQrMap = checkQr(headers,image,uuid);
                        result_code =  StringUtils.isEmpty(checkQrMap.get("result_code"))?"":String.valueOf(checkQrMap.get("result_code"));
                        uamtk =  StringUtils.isEmpty(checkQrMap.get("uamtk"))?"":String.valueOf(checkQrMap.get("uamtk"));
                    if(!result_code.equals("2")){
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                }
                flag = true;
                logger.info("二维码登录之后。。。。。。。。。。。。。。。。。。。");
                getAllCookies(this.cookieStore);
            }
        }
        return flag;
    }

    public void genereteQr(JDialog dialog,String image){
        String codeName = System.currentTimeMillis()+".jpg";
        String savePath=CommonUtil.sessionPath+CommonUtil.codePath;
        File sf=new File(savePath);
        if(!sf.exists()){
            sf.mkdirs();
        }
        ImageUtil.GenerateImage(image,savePath+File.separator+codeName);
        String fileName = savePath+File.separator+codeName;
        JLabel label = new JLabel(new ImageIcon(fileName),
                JLabel.CENTER);

        label.setBounds(0, 0, 200, 200);
        label.setVerticalAlignment(SwingConstants.TOP);
        dialog.setBounds(0,10,300,300);
        dialog.add(label);
        dialog.setModal(true);
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().width)/2;
        int y = (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().height)/2;
        dialog.setLocation(x,y);
        dialog.setVisible(true);
//        label.addMouseListener(new RecordListener());
       /* Container container = dialog.getContentPane();
        dialog.setAlwaysOnTop(true);
//        container.setLayout(null);
        container.add(label);
        container.setVisible(true);*/

        /*JOptionPane.showConfirmDialog(null, label,
                "请输入验证码", JOptionPane.DEFAULT_OPTION);*/

    }

    public Map<String,Object> createQr(Header[] headers){
        Map<String,Object> map = new HashMap<String,Object>();
        if(null== headers || headers.length<3){
            headers = new Header[3];
            headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            headers[1] = new BasicHeader("Host","kyfw.12306.cn");
            headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
        }
        HttpUriRequest createQr = RequestBuilder.post()
                .setUri("https://"+hosts+"/passport/web/create-qr64")
                .addHeader(headers[0]).addHeader(headers[1])
                .addHeader(headers[2])
                .addParameter("appid","otn")
                .setConfig(requestConfig)
                .build();
        CloseableHttpResponse response2 = null;
        try {
            response2 = httpclient.execute(createQr);
            HttpEntity entity = response2.getEntity();
            String jsonStr= EntityUtils.toString(entity);
            map = jsonBinder.fromJson(jsonStr,Map.class);

        }catch (Exception e){
            logger.error("生成二维码时出错",e);
        }finally {
            closeResponse(response2);
        }

        return map;
    }

    public Map<String,Object> checkQr(Header[] headers,String image,String uuid){
        Map<String,Object> map = new HashMap<String,Object>();
        if(null== headers || headers.length<3){
            headers = new Header[3];
            headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            headers[1] = new BasicHeader("Host","kyfw.12306.cn");
            headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
        }
        HttpUriRequest checkQr = RequestBuilder.post()
                .setUri("https://"+hosts+"/passport/web/checkqr")
                .addHeader(headers[0]).addHeader(headers[1])
                .addHeader(headers[2])
                .addParameter("appid","otn")
                .addParameter("uuid",uuid)
                .setConfig(requestConfig)
                .build();
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(checkQr);
            HttpEntity entity = response.getEntity();
            String jsonStr= EntityUtils.toString(entity);
            map = jsonBinder.fromJson(jsonStr,Map.class);

        }catch (Exception e){
            logger.error("校验二维码时出错",e);
        }finally {
            closeResponse(response);
        }

        return map;
    }

}
