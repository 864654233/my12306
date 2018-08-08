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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.List;

/**
 * A example that demonstrates how HttpClient APIs can be used to perform
 * form-based logon.
 */
public class ClientFormLogin {

    public static void main(String[] args) throws Exception {
        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        try {

            HttpUriRequest login = RequestBuilder.post()
//                    .setUri(new URI("http://user.ishowchina.com/loginApp"))
//                    .addParameter("password", "190D48EF2239D5309769D0E186AB927E8397BBA5211ED1769B4A9AAAA6E9E1AD40469F8630F407A0384061DB63898D5AF28E04849FCB652BE61AC5C452D3608A5179B8C9BFCA18CA99267F9F1E93163512E452B1B73E9180B887CB516ED9B296E96B7F4531DE7333D686B447D17A2A442A4CF22B2A36A7B4C3E8E179260A43B2")
//                    .addParameter("username", "18500072208").addParameter("logintype", "3")
                    .setUri(new URI("http://user.ishowchina.com/login"))
//                    .addParameter("password", "a123456")
//                    .addParameter("username", "xiaoq").addParameter("logintype", "3")
                    .addParameter("password", "034a58c0a3ed140f656df15303624b13e09085049ad3aea410bc713e5453251d72351fca9c550dfecbdb55b8fbbe00612c1c03ba3254617378fcd6ac7d7c88b357ad3a8c2b26c5658ff118b28f82bcf5cf199d52832596f26ce92a4c1585af5e8ab11d9ad785181aa9d366c38c5899c4cdabf46b63fea19abc1379123057b094")
                    .addParameter("username", "dsadmin").addParameter("logintype", "3")
                    .build();
            CloseableHttpResponse response2 = httpclient.execute(login);
            try {
                HttpEntity entity = response2.getEntity();
                Header[] headers = response2.getAllHeaders();
                System.out.println("开始获取header");
                for(Header h:headers){
                    System.out.println(h.getName()+":"+h.getValue());
                }
                System.out.println("结束获取header");

                System.out.println("Login form get: " + response2.getStatusLine());
                EntityUtils.consume(entity);

                System.out.println("Post logon cookies:");
                List<Cookie> cookies = cookieStore.getCookies();
                if (cookies.isEmpty()) {
                    System.out.println("None");
                } else {
                    for (int i = 0; i < cookies.size(); i++) {
                        System.out.println("- " + cookies.get(i).toString());
                    }
                }
                
            } finally {
                response2.close();
            }
//


            HttpGet httpgetu = new HttpGet("http://user.ishowchina.com/authorization/user/list");
            CloseableHttpResponse responseu = httpclient.execute(httpgetu);
            try {
                HttpEntity entity = responseu.getEntity();

                System.out.println("Login form get: " + responseu.getStatusLine());
                System.out.println(EntityUtils.toString(entity));
//                EntityUtils.consume(entity);

                System.out.println("Initial set of cookies:");
                List<Cookie> cookies = cookieStore.getCookies();
                if (cookies.isEmpty()) {
                    System.out.println("None");
                } else {
                    for (int i = 0; i < cookies.size(); i++) {
                        System.out.println("- " + cookies.get(i).toString());
                    }
                }
            } finally {
                responseu.close();
            }



//            List<Cookie> cookies = cookieStore.getCookies();
//            for (Cookie cookie : cookies) {
//            	System.out.println(cookie.getName()+","+cookie.getPath());
//			}
            HttpGet httpget = new HttpGet("http://cloudmap.ishowchina.com/tk/gds/storage/dataSet/listAll");
            CloseableHttpResponse response1 = httpclient.execute(httpget);
            try {
                HttpEntity entity = response1.getEntity();

                System.out.println("Login form get: " + response1.getStatusLine());
                System.out.println(EntityUtils.toString(entity));
//                EntityUtils.consume(entity);

                System.out.println("Initial set of cookies:");
                List<Cookie> cookies = cookieStore.getCookies();
                if (cookies.isEmpty()) {
                    System.out.println("None");
                } else {
                    for (int i = 0; i < cookies.size(); i++) {
                        System.out.println("- " + cookies.get(i).toString());
                    }
                }
            } finally {
                response1.close();
            }
        } finally {
            httpclient.close();
        }
            
    }
}
