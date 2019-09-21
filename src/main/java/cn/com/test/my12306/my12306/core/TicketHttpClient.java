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

import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TicketHttpClient {


    private static Logger logger = LogManager.getLogger(TicketHttpClient.class);

    public static CloseableHttpClient getClient() {
        return getClient(null);
    }

    public static CloseableHttpClient getClient(BasicCookieStore cookieStore) {
        CloseableHttpClient httpClient = null;
        try {
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();


            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();

            httpClientBuilder = httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder = httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false));
            HttpHost proxy = new HttpHost("106.14.162.110", 8080, "http");
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
//            httpClientBuilder.setRoutePlanner(routePlanner);
           /* httpClientBuilder.setRetryHandler((exception, executionCount, context) -> {
                if (executionCount > 3) {
                    logger.warn("Maximum tries reached for client http pool ");
                    return false;
                }

                if (exception instanceof NoHttpResponseException     //NoHttpResponseException 重试
                        || exception instanceof ConnectTimeoutException //连接超时重试
//              || exception instanceof SocketTimeoutException    //响应超时不重试，避免造成业务数据不一致
                ) {
                    logger.warn("NoHttpResponseException on " + executionCount + " call");
                    return true;
                }
                return false;
            });*/
            if(null!=cookieStore){
                httpClientBuilder.setDefaultCookieStore(cookieStore);
            }
            httpClientBuilder.setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            httpClient = httpClientBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpClient;
    }

    public static CloseableHttpClient getRetryClient() {
        return getRetryClient(null);
    }

    public static CloseableHttpClient getRetryClient(BasicCookieStore cookieStore) {
        CloseableHttpClient httpClient = null;
        try {
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();

            httpClientBuilder = httpClientBuilder.setSSLContext(sslContext);
            if(null!=cookieStore){
                httpClientBuilder.setDefaultCookieStore(cookieStore);
            }
            httpClientBuilder = httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(3,false));
            httpClientBuilder.setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler());
            httpClient = httpClientBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpClient;
    }
}
