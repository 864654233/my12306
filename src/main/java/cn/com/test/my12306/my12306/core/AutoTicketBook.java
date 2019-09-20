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

import cn.com.test.my12306.my12306.core.util.FileUtil;
import cn.com.test.my12306.my12306.core.util.JsonBinder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;


/**
 * 下单参考 ：https://www.jianshu.com/p/6b1f94e32713
 * 和    http://www.cnblogs.com/small-bud/p/7967650.html
 * 功能同AsyncTicketBook,只是使用不同的实现方式（使用Runnable实现）
 */
public class AutoTicketBook implements  Runnable{

    public JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);

    private BlockingQueue<Map<String,String>> queue ;
    private CloseableHttpClient httpclient;
    private BasicCookieStore cookieStore;
    private Header[] headers;
    private ClientTicket ct;
    private String bookRancode="";
    private static Logger logger = LogManager.getLogger(AutoTicketBook.class);

    CommonUtil commonUtil  ;

    public AutoTicketBook(ClientTicket ct, BlockingQueue<Map<String, String>>queue, CloseableHttpClient httpclient, Header[] headers, BasicCookieStore cookieStore) {
        this.ct = ct;
        this.queue = queue;
        this.httpclient = httpclient;
        this.headers = headers;
        this.cookieStore = cookieStore;
        this.commonUtil = ct.commonUtil;
        if(this.headers.length!=8){
            this.headers = new BasicHeader[8];
            this.headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            this.headers[1] = new BasicHeader("Host","kyfw.12306.cn");
            this.headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");
            this.headers[3] = new BasicHeader("Accept","*/*");
            this.headers[4] = new BasicHeader("Accept-Encoding","gzip, deflate, br");
            this.headers[5] = new BasicHeader("Accept-Language","zh-CN,zh;q=0.9");
            this.headers[6] = new BasicHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
//            this.headers[7] = new BasicHeader("Origin","https://kyfw.12306.cn");
            this.headers[7] = new BasicHeader("Cache-Control","no-cache");
//            this.headers[8] = new BasicHeader("X-Requested-With","XMLHttpRequest");
        }else{
            this.headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");
        }
    }


    public void resetHeaders(){
        this.headers = headers;
        if(this.headers.length!=8){
            this.headers = new BasicHeader[8];
            this.headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
            this.headers[1] = new BasicHeader("Host","kyfw.12306.cn");
            this.headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");
            this.headers[3] = new BasicHeader("Accept","*/*");
            this.headers[4] = new BasicHeader("Accept-Encoding","gzip, deflate, br");
            this.headers[5] = new BasicHeader("Accept-Language","zh-CN,zh;q=0.9");
            this.headers[6] = new BasicHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
//            this.headers[7] = new BasicHeader("Origin","https://kyfw.12306.cn");
            this.headers[7] = new BasicHeader("Cache-Control","no-cache");
//            this.headers[8] = new BasicHeader("X-Requested-With","XMLHttpRequest");
        }else{
            this.headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        String orderId = "";
        Map<String, String> trainMap = null;
        try {
            start:
            while (orderId.equals("") && (trainMap = queue.take()) != null) {
                resetHeaders();
                this.headers[2] = new BasicHeader("Referer", "https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc");

                //获取的整个车次信息
                logger.info("有票了，开始预定");
                //获取所有乘客信息
                List<Map<String, String>> userList = getPassenger("");
                if(null==userList || userList.size()<=0){
                    //未获取到用户信息 重新登陆
                    ct.resetCookiesFile();
                    ct.resetCookieStore();
                    headers = new BasicHeader[3];
                    headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                    headers[1] = new BasicHeader("Host","kyfw.12306.cn");
                    headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
                    ct.login(headers);
                    while(!ct.isReady){
                        Thread.sleep(100);
                    }
                    orderId ="";
//                    this.ct = ct;
                    this.queue = ct.queue;
                    this.httpclient = ct.httpclient;
                    continue start ;
                }
                Map<String, String> subMap = autoSubmi(trainMap.get("secret"), trainMap.get("toBuySeat"), userList);
                if (subMap.size() > 0) {
                    if (subMap.get("ifShowPassCode").equals("Y")) {
                        StringBuffer saveStr = new StringBuffer(System.currentTimeMillis()+" bookTickets:")
                                .append("Date:" + commonUtil.getBuyDate() + "|")
                                .append("trainInfo:" + trainMap).append("\n");
                        FileUtil.saveTo(saveStr.toString(), "D:\\neesPassCode.txt");
                        //识别验证码
                        //没有globalRepeatSubmitToken 暂时先不处理
                        //获取验证码
                        boolean checkedCode = false;
                        while (!checkedCode) {
                            //获取验证码
                            String valicode = getCode("", headers);
                            logger.info("验证码：" + valicode);

                            //校验验证码
                            HttpUriRequest checkCode = RequestBuilder.post()
                                    .setUri(new URI("https://kyfw.12306.cn/otn/passcodeNew/checkRandCodeAnsyn"))
                                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                                    .addParameter("randCode", valicode)
                                    /* .addParameter("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken)*/
                                    .addParameter("rand", "randp")
                                    .build();
                            CloseableHttpResponse response = httpclient.execute(checkCode);

                            Map<String, Object> rsmap = null;
                            try {
                                HttpEntity entity = response.getEntity();
                                String responseBody = EntityUtils.toString(entity);
                                rsmap = this.jsonBinder.fromJson(responseBody, Map.class);
                                logger.info("校验：" + response.getStatusLine().getStatusCode() + " " + entity.getContent() + " abc " + EntityUtils.toString(entity));
                                if ((rsmap.get("status") + "").equalsIgnoreCase("true")) {
                                    Map<String, Object> dataMap = (Map<String, Object>) rsmap.get("data");
                                    String msg = rsmap.get("msg") + "";
                                    if (msg.equalsIgnoreCase("TRUE")) {
                                        logger.info("验证码校验通过");
                                        checkedCode = true;
                                    }
                                } else {
                                    logger.info("验证码校验没有通过");
                                }
                            } catch (Exception e) {
                                logger.info("验证码校验没有通过");
                                e.printStackTrace();
                            } finally {
                                response.close();
                            }
                        }
                    }
                    //获取余票数
                    int tickets = getQueueCountAsync(trainMap);
                    //确认订单信息
                    boolean confirmFlag = confirmSingleAsys(subMap.get("key_check_isChange"), trainMap.get("toBuySeat"), trainMap, userList);
                    if(!confirmFlag){
                        ct.resetCookiesFile();
                        ct.resetCookieStore();
                        headers = new BasicHeader[3];
                        headers[0] =new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0");
                        headers[1] = new BasicHeader("Host","kyfw.12306.cn");
                        headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/resources/login.html");
                        ct.login(headers);
                        while(!ct.isReady){
                            Thread.sleep(100);
                        }
                        this.queue = ct.queue;
                        this.httpclient = ct.httpclient;
                        continue start ;
                    }
                    orderId = waitOrder(null);
                    orderId = orderId.equals("null") ? "" : orderId;
                    logger.info("获取的订单Id：{}", orderId);
                    if (!orderId.equals("")) {
                        //订票成功 退出程序
                        logger.info("购票成功，订单Id：{},赶紧支付去吧", orderId);
//                          new TipTest("","","订票成功，订单号："+orderId);
                        ct.sendSuccessMail("购票成功，订单ID：" + orderId);
                        System.exit(0);
                    } else {
                        //重新开始预定
                        continue start;
                    }

                }


            }
            Thread.sleep(200L);
        } catch (Exception e) {
//               e.printStackTrace();
            logger.error("预定时出错", e);
        }
    }

    /**
     * 获取验证码弹窗
     * @param url
     * @return
     * @throws IOException
     */
    public String getCode(String url,Header[] headers) throws IOException {
        //JFrame frame = new JFrame("验证码");
        this.bookRancode="";
        logger.info("获取验证码的地址："+url);
//        new TipTest("","","请输入验证码");
        JLabel label = new JLabel(new ImageIcon(getCodeByte(url,headers)),
                JLabel.CENTER);

        label.setBounds(0, 0, 295, 220);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.addMouseListener(new RecordListener());

        JOptionPane.showConfirmDialog(null, label,
                "请输入验证码", JOptionPane.DEFAULT_OPTION);
		/*InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		String rd = br.readLine();*/
        //frame.dispose();
        return this.bookRancode;
    }


    /**
     * 获取指定url的验证码图片字节信息
     * @param url
     * @return
     */

    public byte[] getCodeByte(String url,Header[] headers) {
        HttpGet get = new HttpGet(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytse = null;
        try {
            HttpGet hget = new HttpGet("https://kyfw.12306.cn/otn/passcodeNew/getPassCodeNew?module=passenger&rand=randp&" + Math.random());
            for(Header h:headers){
                hget.addHeader(h);
            }
//                CloseableHttpResponse response = httpclient.execute(new HttpGet("https://"+hosts+"/passport/captcha/captcha-image?login_site=E&module=login&rand=sjrand&" + Math.random()));
            CloseableHttpResponse response = httpclient.execute(hget);

            HttpEntity entity = response.getEntity();
            bytse=  EntityUtils.toByteArray(entity);
            EntityUtils.consume(entity); //Consume response content
        }catch(Exception e ){
            e.printStackTrace();
        }
        return bytse;

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
            bookRancode+=bookRancode.equals("")?x+","+(y-30):","+x+","+(y-30);
            logger.info(x+","+y+"  rancode:"+bookRancode);
        }
    }

    public void resetRancode(){
        this.bookRancode = "";
    }

    /**
     * 获取乘客列表
     * @return
     */
    public List<Map<String,String>> getPassenger(String token){
        CloseableHttpResponse response=null;
        List<Map<String,String>> users =null;
        try {
            Header headerAjax = new BasicHeader("X-Requested-With","XMLHttpRequest");
            HttpUriRequest getPassenger = RequestBuilder.post()
//                .setUri(new URI("https://kyfw.12306.cn/otn/confirmPassenger/getPassengerDTOs"))
                    .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/getPassengerDTOs"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                    .addHeader(headers[7])
                    .addHeader(headerAjax)
                    /* .addParameter("REPEAT_SUBMIT_TOKEN", token)*/
                    .addParameter("_json_att", "")
                    .build();
            response = httpclient.execute(getPassenger);

            Map<String, Object> rsmap = null;

            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            rsmap = jsonBinder.fromJson(responseBody, Map.class);
            if (rsmap.get("status").toString().equalsIgnoreCase("true")) {
                Map<String,Object> dataMap = (Map<String,Object>)rsmap.get("data");
               /*
                code	10
                passenger_name	张无忌
                sex_code	M
                sex_name	男
                born_date	2017-08-25 00:00:00
                country_code	CN
                passenger_id_type_code	1
                passenger_id_type_name	二代身份证
                passenger_id_no	130XXXX
                passenger_type	1
                passenger_flag	0
                passenger_type_name	成人
                mobile_no
                phone_no
                email
                address
                postalcode
                first_letter	ZJQ
                recordCount	13
                total_times	99
                index_id	0
                */
                users=(List<Map<String,String>>)dataMap.get("normal_passengers");
                logger.info("获取用户乘客信息完成"+responseBody);

            } else {
                logger.info("获取用户乘客信息失败"+responseBody);
            }
        }catch (Exception e){
            logger.info("获取用户乘客信息失败1");
            e.printStackTrace();
        }finally {
            try{
                response.close();
            }catch (Exception e){

            }
        }
        return users;
    }

    /**
     * 自动预订 掉线后不能自动提交 （模拟官方订票助手）
     * @param seat 要预定的席别
     *  0 失败；1：成功；2：可能被封或退出登陆
     */
    public  Map<String,String> autoSubmi(String secretStr,String seat, List<Map<String,String>> userList){
        CloseableHttpResponse response=null;
        Map<String,String> rsMap = new HashMap<>();
        try {
            Header headerAjax = new BasicHeader("X-Requested-With","XMLHttpRequest");
            String[] users = commonUtil.getPassengerNames().split(",");
            String oldPassengerStr="";//姓名，证件类别，证件号码，用户类型
            String passengerTicketStr="";//座位类型，0，车票类型，姓名，身份正号，电话，N（多个的话，以逗号分隔）
            for(Map<String,String> u:userList){
                for(String u1:users){
                    if(u1.equals(u.get("passenger_name"))){
//                        oldPassengerStr+=URLEncoder.encode(u.get("passenger_name"),"utf-8")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("passenger_type")+"_";
//                        passengerTicketStr+=commonUtil.getSeatMap().get(seat)+",0,1,"+URLEncoder.encode(u.get("passenger_name"),"utf-8")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("mobile_no")+",N_";
                        oldPassengerStr+=u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("passenger_type")+"_";
                        passengerTicketStr+= commonUtil.getSeatMap().get(seat)+",0,1,"+u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("mobile_no")+",N_";
                    }
                }
            }
            passengerTicketStr=passengerTicketStr.endsWith("_")?passengerTicketStr.substring(0,passengerTicketStr.length()-1):passengerTicketStr;
            HttpUriRequest autoSubmi = RequestBuilder.post()
//                    .setUri(new URI("https://kyfw.12306.cn/otn/confirmPassenger/autoSubmitOrderRequest"))
                    .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/autoSubmitOrderRequest"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                    .addHeader(headerAjax)
                    .addParameter("bed_level_order_num", "000000000000000000000000000000")
                    .addParameter("cancel_flag", "2")
                    .addParameter("oldPassengerStr", oldPassengerStr)
                    .addParameter("passengerTicketStr",passengerTicketStr)
                    .addParameter("purpose_codes", "ADULT")
                    .addParameter("query_from_station_name",  URLEncoder.encode(commonUtil.getBuyFrom(),"utf-8"))
                    .addParameter("query_to_station_name",  URLEncoder.encode(commonUtil.getBuyTo(),"utf-8"))
                    .addParameter("secretStr",  secretStr)
                    .addParameter("tour_flag",  "dc")
                    .addParameter("train_date",  commonUtil.getBuyDate())
                    .build();
            response = httpclient.execute(autoSubmi);

            Map<String, Object> responseMap = null;

            HttpEntity entity = response.getEntity();
            responseMap = jsonBinder.fromJson(EntityUtils.toString(entity), Map.class);
            if (responseMap.get("status").toString().equalsIgnoreCase("true")) {
                Map<String,Object> dataMap = (Map<String,Object>)responseMap.get("data");
                if(dataMap.get("submitStatus").toString().equals("false")){
                    String errMsg = dataMap.get("errMsg").toString();
                    logger.warn(errMsg);
                    rsMap.put("status","false");
                }else{
                    String drs=dataMap.get("result")+"";
                    String ifShowPassCode=dataMap.get("ifShowPassCode")+"";//是否需要验证码 Y需要 N不需要
                    String ifShowPassCodeTime=dataMap.get("ifShowPassCodeTime")+"";//不知道是否要等待这么久2801

                    rsMap.put("ifShowPassCode",ifShowPassCode);
                    rsMap.put("ifShowPassCodeTime",ifShowPassCodeTime);
                    String[] rsArr = drs.split("#");
                    String leftTicket = rsArr[2];
                    String train_location=dataMap.get(rsArr[0])+"";
                    rsMap.put("key_check_isChange",rsArr[1]);

                    rsMap.put("leftTicket",leftTicket);
                    logger.info("key_check_isChange：{}，leftTicket：{},location:{}",rsArr[1],leftTicket,train_location);
                    rsMap.put("status","true");
                }

                //post https://kyfw.12306.cn/otn/confirmPassenger/confirmSingleForQueueAsys 生成车票 可能会302


                //get https://kyfw.12306.cn/otn/confirmPassenger/queryOrderWaitTime?random=1517580650391&tourFlag=dc&_json_att= 查询订单信息

                logger.info("自动预定成功：{}",rsMap);
            } else {
                logger.info("自动预订失败:{}",responseMap);
                String msg = responseMap.get("messages")+"";
                if(msg.contains("您还有未处理的订单")){
                    logger.info(msg);
                    ct.sendSuccessMail("您还有未处理的订单");
                    System.exit(0);
                }
            }
        }catch (Exception e){
            logger.error("自动预订出错",e);
        }finally {
            try{
                response.close();
            }catch (Exception e){

            }
        }
        return rsMap;
    }

    /**
     * 檢查用户在线状态
     *
     * @return true 在线;false 掉线了
     */
    public  boolean checkUser(){
        CloseableHttpResponse response=null;
        try{


            HttpUriRequest checkUser = RequestBuilder.post()
//                .setUri(new URI("https://kyfw.12306.cn/otn/login/checkUser"))
                    .setUri(new URI("https://"+ct.hosts+"/otn/login/checkUser"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                    .addParameter("_json_att", "")
                    .build();
            response = httpclient.execute(checkUser);

            Map<String, Object> rsmap = null;

            HttpEntity entity = response.getEntity();
            rsmap = jsonBinder.fromJson(EntityUtils.toString(entity), Map.class);
            if (rsmap.get("status").toString().equals("true")) {
                Map<String,Object> dataMap = (Map<String,Object>)rsmap.get("data");
                return (boolean) dataMap.get("flag");

            }
        }catch (Exception e){
            logger.error("检查用户状态失败",e);
        }finally {
            try{
                response.close();
            }catch (Exception e){

            }
        }
        return false;
    }



    public String getGMT(String date){
        String str="";
        TimeZone tz = TimeZone.getTimeZone("ETC/GMT-8");
        TimeZone.setDefault(tz);
//		Calendar cal = Calendar.getInstance();
        Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
        Date dd;
        SimpleDateFormat shortSdf = new SimpleDateFormat("yyyy-MM-dd");;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            dd = shortSdf.parse(date);
            cal.setTime(dd);
            str = sdf.format(cal.getTime());
            return str+"+0800 (中国标准时间)";
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    //https://github.com/l107868382/tickets/blob/master/src/main/java/com/tickets/tickets/service/impl/TicketsServiceImpl.java


    public String waitOrder(String token) {
        String orderId = "";
        String waitTime = "0";
        String message ="";
        try {
//                Thread.sleep(2000);
            String tokenParam = "";
            if(!StringUtils.isEmpty(token)){
                tokenParam="&REPEAT_SUBMIT_TOKEN="+token;
            }
            Header headerAjax = new BasicHeader("X-Requested-With","XMLHttpRequest");
//            while(orderId.equals("")){
            while (Integer.parseInt(waitTime)>=0) { //是不是-1啊 忘记了
                HttpUriRequest waitOrder = RequestBuilder.get()
//                        .setUri("https://kyfw.12306.cn/otn/confirmPassenger/queryOrderWaitTime?random="+System.currentTimeMillis()+"&tourFlag=dc&_json_att" + tokenParam)
                        .setUri("https://"+ct.hosts+"/otn/confirmPassenger/queryOrderWaitTime?random="+System.currentTimeMillis()+"&tourFlag=dc&_json_att" + tokenParam)
                        .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                        .addHeader(headerAjax)
                        .build();

                CloseableHttpResponse response = httpclient.execute(waitOrder);
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                Map<String, Object> rsmap = jsonBinder.fromJson(responseBody, Map.class);
                if (rsmap.get("status").toString().equals("true")) {
                    Map<String, Object> data = (Map<String, Object>) rsmap.get("data");
                    waitTime = data.get("waitTime") + "";
                    String waitCount = data.get("waitCount") + "";
                    Object oId = data.get("orderId");
                    if(!StringUtils.isEmpty(oId)){
                        orderId = oId.toString();
                    }
                    message = data.get("msg") + "";
                    if(null!=message){//已有订单
                        if(message.toString().contains("行程冲突")){
                            logger.info(message.toString());
                            ct.sendSuccessMail("行程冲突");
                            System.exit(0);
                        }
                        if(message.toString().contains("取消次数过多")){
                            logger.info(message.toString());
                            ct.sendSuccessMail("取消次数过多,请切换账号");
                            System.exit(0);
                        }
                        logger.info("dddddd"+data.get("msg"));
                        //只打印消息 继续从queue里获取其他票 尝试下单
//                        System.exit(0);
                    }
                    if(Integer.valueOf(waitTime)<0){
                        logger.info(data);
                    }
                    logger.info("前面" + waitCount + "人，需等待：" + waitTime + "");
                    if(Integer.valueOf(waitTime)<0 && StringUtils.isEmpty(oId)){
                        logger.info("rrrrrrrrrrrrrrrrrrrrrrrrrrrr");
                        waitTime="1";
                    }

                    Thread.sleep(3000);
                }
            }
            if(StringUtils.isEmpty(orderId)){
                logger.info("获取订单号失败："+message);
            }
        } catch (Exception e) {
            logger.error("查询订单号失败",e);
        }
        return orderId;

    }












    /**
     * 自动提交-获取排队和余票信息
     * @param map
     * @return 余票不够时的提示信息，空表示余票够
     */
    public int getQueueCountAsync(Map<String,String> map){
        CloseableHttpResponse response=null;
        Map<String,Object> rsMap= new HashMap<String,Object>();
        int tickets = 0;
        String responseBody ="";
        try{
            HttpUriRequest confirm = RequestBuilder.post()
//                    .setUri(new URI("https://kyfw.12306.cn/otn/confirmPassenger/getQueueCountAsync"))
                    .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/getQueueCountAsync"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                    .addParameter("fromStationTelecode", map.get("fromStationTelecode"))
                    .addParameter("toStationTelecode", map.get("toStationTelecode"))
                    .addParameter("leftTicket", map.get("leftTicket"))
                    .addParameter("purpose_codes", "ADULT")
                    .addParameter("seatType", commonUtil.getSeatMap().get(map.get("toBuySeat")))
                    .addParameter("stationTrainCode", map.get("chehao"))
                    .addParameter("train_date", getGMT(commonUtil.getBuyDate()))//时间格式待定 Sun+Feb+25+2018+00:00:00+GMT+0800
                    .addParameter("train_location", map.get("train_location"))
                    .addParameter("train_no", map.get("train_no"))
                    .addParameter("_json_att", "")
                    .build();
            response = httpclient.execute(confirm);


            HttpEntity entity = response.getEntity();

            if(response.getStatusLine().getStatusCode()==200){

                responseBody =EntityUtils.toString(entity);
                logger.info("查询排队和余票成功"+responseBody);
                Map<String, Object> rsmap = jsonBinder.fromJson(responseBody, Map.class);
                if (rsmap.get("status").toString().equals("true")) {
                    rsMap=(Map<String, Object>)rsmap.get("data");
                    /*
                    data.count=排队人数
                    data.ticket=余票数
                     */
                    String ticket = String.valueOf(rsMap.get("ticket"));
                    int yp = Integer.valueOf(ticket.split(",")[0]);
                    int wzyp =  Integer.valueOf(ticket.split(",")[1]);
                    logger.info("该车次还有余票：{}张，无座余票：{}张",yp,wzyp);
                    tickets = yp;
                }
            }else{
                logger.info("查询排队和余票失败11111");
            }

        }catch (Exception e){
            logger.info("查询排队和余票失败,responseBody:{}",responseBody);
            logger.error(e);
        }finally {
            try{
                response.close();
            }catch (Exception e){

            }
        }
        return tickets;
    }

    /**
     * 单程票提交确认
     * 往返票地址 为https://kyfw.12306.cn/otn/confirmPassenger/confirmGoForQueue
     * 请求体相同
     *
     */
    public boolean confirmSingleAsys(String key_check_isChange,String seat,Map<String,String> map, List<Map<String,String>> userList){
        CloseableHttpResponse response=null;
        String  responseBody="";
        try{
            Header headerAjax = new BasicHeader("X-Requested-With","XMLHttpRequest");
            String[] users = commonUtil.getPassengerNames().split(",");
            String oldPassengerStr="";//姓名，证件类别，证件号码，用户类型
            String passengerTicketStr="";//座位类型，0，车票类型，姓名，身份正号，电话，N（多个的话，以逗号分隔）
            for(Map<String,String> u:userList){
                for(String u1:users){
                    if(u1.equals(u.get("passenger_name"))){
                        oldPassengerStr+=u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("passenger_type")+"_";
                        passengerTicketStr+= commonUtil.getSeatMap().get(seat)+",0,1,"+u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("mobile_no")+",N_";
                    }
                }
            }
            passengerTicketStr=passengerTicketStr.endsWith("_")?passengerTicketStr.substring(0,passengerTicketStr.length()-1):passengerTicketStr;

            HttpUriRequest confirm = RequestBuilder.post()
//                    .setUri(new URI("https://kyfw.12306.cn/otn/confirmPassenger/confirmSingleForQueueAsys"))
                    .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/confirmSingleForQueueAsys"))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4]).addHeader(headers[5]).addHeader(headers[6])
                    .addHeader(headerAjax)
                    .addParameter("passengerTicketStr", passengerTicketStr)
                    .addParameter("oldPassengerStr", oldPassengerStr)
                    .addParameter("randCode", "")
                    .addParameter("purpose_codes", "ADULT")
                    .addParameter("key_check_isChange", key_check_isChange)
                    .addParameter("leftTicketStr", map.get("leftTicket"))
                    .addParameter("train_location", map.get("train_location"))
//                    .addParameter("whatsSelect", "1")
                    .addParameter("choose_seats", "")
                    .addParameter("seatDetailType", "")
                    .addParameter("_json_att", "")
                    .build();
            response = httpclient.execute(confirm);


            HttpEntity entity = response.getEntity();
            responseBody =EntityUtils.toString(entity);
            if(response.getStatusLine().getStatusCode()==200){

                Map<String, Object> rsmap = jsonBinder.fromJson(responseBody, Map.class);
                if (rsmap.get("status").toString().equals("true")) {
                    Map<String, Object> data=(Map<String, Object>)rsmap.get("data");
                    String subStatus = data.get("submitStatus")+"";//true为成功 false为失败 需要查看errMsg
                    if(subStatus.equals("true")){
                        logger.info("确认提交订单成功"+responseBody);
                        return true;
                    }else{
                        String errMsg =data.get("errMsg")+"";
                        logger.info("确认提交订单失败22222"+errMsg+" 返回内容："+responseBody);
                        if(errMsg.contains("未完成")){
                            return true;
                        }
                        return false;
                    }
                }else{
                    logger.info("确认提交订单失败111111"+responseBody);
                    return false;
                }

            }else{
                logger.info("确认提交订单失败"+responseBody);
                return false;
            }

        }catch (Exception e){
            logger.error("确认提交订单失败,responseBody:{}",responseBody,e);
        }finally {
            try{
                response.close();
            }catch (Exception e){

            }
        }
        return false;
    }

}
