
package cn.com.test.my12306.my12306.core;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HtmlParser {

   private static BasicCookieStore cookieStore = new BasicCookieStore();
    private static CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
                .build();

    public String getResponseStr(String uri){
        String resStr = null;
        try {
            Header[] headers = new BasicHeader[5];
            headers[0] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            headers[1] = new BasicHeader("Host", "www.xuanshu.com");
            headers[2] = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers[3] = new BasicHeader("Accept-Encoding", "gzip, deflate, br");
            headers[4] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            HttpUriRequest initPage1 = RequestBuilder.get()//.post()
                    .setUri(new URI(uri))
                    .addHeader(headers[0]).addHeader(headers[1]).addHeader(headers[2]).addHeader(headers[3]).addHeader(headers[4])
                    .build();
            CloseableHttpResponse response3 = null;

            try {
                response3 = httpclient.execute(initPage1);

                HttpEntity entity = response3.getEntity();
                resStr = EntityUtils.toString(entity);


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    response3.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return resStr;
    }

    public Map<String,String> parse2Map(String uri){
        Map<String,String> map =new HashMap<String,String>();
        String baseUrl="https://www.xuanshu.com/book/20651/";
        try{
//        Document doc = Jsoup.parse(str);
        String url = baseUrl+uri;
        Document doc = Jsoup.parse(new URL(url).openStream(), "UTF-8", url);
        String title = doc.select("div[class=txt_cont]").get(0).select("h1").text();
        String content = doc.select("div[id=content1]").get(0).text();
        String nextUrl = doc.select("div[class=txt_lian2]").get(0).select("a").get(3).attr("href");
        map.put("title",title);
        map.put("content",content);
        map.put("nextUrl",nextUrl);
        }catch (Exception e){
            e.printStackTrace();
        }
        return map;
    }

    public void ss(){
        String uri = "https://www.xuanshu.com/book/20651/5685054.html";
        String res = getResponseStr(uri);
        Map<String,String> resMap =parse2Map(res);

    }

    public void writeFile( Map<String,String> resMap ) {
        /*try {
            PrintWriter pw=new PrintWriter("D://1.txt");
            pw.write(resMap.get("title")+"\n");
//            pw.write(resMap.get("\n"));
            pw.write(resMap.get("content"));
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("D://1.txt", true)));
            out.write(resMap.get("title") + "\r\n");
            out.write(resMap.get("content") + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public static void main(String[] args){
        HtmlParser  parser= new HtmlParser();
        String startPage="5685026.html";
        String endPage="7087835.html";
        Map<String,String> resMap = null;
        System.out.println("开始执行");
        if(null==resMap){
            resMap= parser.parse2Map(startPage);
            parser.writeFile(resMap);
        }
        while(null!=resMap && !resMap.get("nextUrl").equals(endPage)){
            resMap= parser.parse2Map(resMap.get("nextUrl"));
            parser.writeFile(resMap);
        }
        if(resMap.get("nextUrl").equals(endPage)){
            resMap= parser.parse2Map(endPage);
            parser.writeFile(resMap);
        }
        System.out.println("执行完了");
    }

}
