package cn.com.test.my12306.my12306.core;

import cn.com.test.my12306.my12306.core.util.JsonBinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TicketUtil {

    public JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
    private static Logger logger = LogManager.getLogger(TicketUtil.class);
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private ClientTicket ct;


    public void closeResponse(CloseableHttpResponse response) {
        try {
            if (null != response) {
                response.close();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 进入下单页面
     *
     * @return token,key_check_isChange
     */
    public String initDc(Header[] headers){
        CloseableHttpResponse response=null;
        String token ="";
        String responseBody = "";
        try{
            headers[2] = new BasicHeader("Referer","https://kyfw.12306.cn/otn/leftTicket/init");
            RequestBuilder initBuilder = RequestBuilder.get();
            initBuilder = initBuilder
                    .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/initDc"))
                    .addParameter("_json_att", "");
            for(Header h:headers){
                initBuilder = initBuilder.addHeader(h);
            }
            HttpUriRequest confirm =initBuilder
                    .build();
            response =  ct.getHttpclient().execute(confirm);



            if(response.getStatusLine().getStatusCode()==200){
                HttpEntity entity = response.getEntity();
                responseBody =EntityUtils.toString(entity);
                logger.info("initDc成功");
                Pattern p=Pattern.compile("globalRepeatSubmitToken \\= '(.*?)';");
                Matcher m=p.matcher(responseBody);
                while(m.find()){
                    token=m.group(1);
                }
                Pattern p1=Pattern.compile("'key_check_isChange':'(.*?)',");
                Matcher m1=p1.matcher(responseBody);
                while(m1.find()){
                    token+=","+m1.group(1);
                }
            }else{
                logger.info("initDc失败 status错误{}",response.getStatusLine().getStatusCode());
            }

        }catch (Exception e){
            logger.info("initDc失败"+responseBody);
            e.printStackTrace();
        }finally {
            closeResponse(response);
        }
        return token;

    }

    /**
     * 获取乘客列表
     * @return
     */
    public List<Map<String,String>> getPassenger(Header[] headers,String token){
        CloseableHttpResponse response=null;
        List<Map<String,String>> users =null;
        try {
            for (int i = 0; i < 5; i++) {


                RequestBuilder builder = RequestBuilder.post();
                builder = builder
                        .setUri(new URI("https://"+ct.hosts+"/otn/confirmPassenger/getPassengerDTOs"))
                        .addParameter("REPEAT_SUBMIT_TOKEN", token)
                        .addParameter("_json_att", "");
                for(Header h:headers){
                    builder.addHeader(h);
                }
                HttpUriRequest checkCode = builder.build();
                response = ct.getHttpclient().execute(checkCode);

                Map<String, Object> rsmap = null;

                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                rsmap = jsonBinder.fromJson(responseBody, Map.class);
                if (null!= rsmap && rsmap.get("status").toString().equalsIgnoreCase("true")) {
                    Map<String,Object> dataMap = (Map<String,Object>)rsmap.get("data");
                    String noLogin = String.valueOf(dataMap.get("noLogin"));
                    if(noLogin.equalsIgnoreCase("true")){
                        return null;
                    }
                    users=(List<Map<String,String>>)dataMap.get("normal_passengers");
                    logger.info("获取用户乘客信息完成"+responseBody);
                    break;
                } else {
                    logger.info("获取用户乘客信息失败"+responseBody);
                }
            }
        }catch (Exception e){
            logger.info("获取用户乘客信息失败1");
            e.printStackTrace();
        }finally {
            closeResponse(response);
        }
        return users;
    }

    public Map<String,String> getPassengerStr(Header[] headers){
        Map<String,String> map = new HashMap<String, String> ();
        String tokens = initDc(headers);
        if(StringUtils.isNotBlank(tokens)){
            List<Map<String,String>> userList = getPassenger(headers,tokens.split(",")[0]);
            if(null!=userList && userList.size()>0){
                String[] users = commonUtil.getPassengerNames().split(",");
                //姓名，证件类别，证件号码，用户类型
                String oldPassengerStr="";
                //座位类型，0，车票类型，姓名，身份正号，电话，N（多个的话，以逗号分隔）
                String passengerTicketStr="";
                for(Map<String,String> u:userList){
                    for(String u1:users){
                        if(u1.equals(u.get("passenger_name"))){
                            oldPassengerStr+=u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("passenger_type")+"_";
                            passengerTicketStr+="{seatType},0,1,"+u.get("passenger_name")+","+u.get("passenger_id_type_code")+","+u.get("passenger_id_no")+","+u.get("mobile_no")+",N,"+u.get("allEncStr")+"_";
                        }
                    }
                }
                passengerTicketStr=passengerTicketStr.endsWith("_")?passengerTicketStr.substring(0,passengerTicketStr.length()-1):passengerTicketStr;
                map.put("oldPassengerStr",oldPassengerStr);
                map.put("passengerTicketStr",passengerTicketStr);
            }
        }
        logger.info("获取乘客信息完成：{}",map);
        return map;
    }

}
