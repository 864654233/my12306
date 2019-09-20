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

import cn.com.test.my12306.my12306.core.util.DateUtil;
import cn.com.test.my12306.my12306.core.util.JsonBinder;
import cn.com.test.my12306.my12306.core.util.mail.MailUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ClientTicketA {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ClientTicket ct;

    @Autowired
    AsyncTicketBook asyncTicketBook;

    @Autowired
    AsyncTicketQuery asyncTicketQuery;


    @Autowired
    MailUtils mailUtils ;
    private static Logger logger = LogManager.getLogger(ClientTicketA.class);

    public JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);

    //是否可以运行查票 用于定时任务
    boolean canRun;
    Boolean isReady = false;//用于查看主线程是否运行完毕

    public BlockingQueue<Map<String,String>> queue = new LinkedBlockingQueue<Map<String, String>>(10);


    public void run() throws Exception {
        canRun = false;
        //是否是正常的预定时间
        if(DateUtil.isNormalTime()){
            canRun =true;
        }else{
            logger.info("维护时间，暂停查询");
            return ;
        }

        try {
            boolean loginFlag=true;
            while( loginFlag){
                loginFlag= !ct.login(null);
                if(!loginFlag){
                    logger.info("ClientTicketA登陆成功，继续执行下面代码");
                    break;
                }

            }
            //设置查询地址 queryA queryZ
            ct.queryInit(null);
            ct.AddCaptchaCookie();
            //启用查票线程
            for(int i =0;i<10;i++){
                asyncTicketQuery.run();
            }
            //启用订票线程
            if(commonUtil.getAutoSub().equals("1")){
//                new Thread(new AutoTicketBook(ct,queue,ct.getHttpclient(),headers,cookieStore)).start();
                for(int i = 0;i<10;i++){
                    asyncTicketBook.run();
                }
            }else{
//                new Thread(new TicketBook(ct,queue,ct.getHttpclient(),headers,cookieStore)).start();
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
//            httpclient.close();
        }
    }

    public void shutdownQueryThread(){
        this.canRun = false;
    }

    public void sendSuccessMail(String msg){
        mailUtils.send(msg);
    }

}
