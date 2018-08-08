package cn.com.test.my12306.my12306.core;

import cn.com.test.my12306.my12306.core.util.JsonBinder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DESCoderTest {

	@Test
	public void testSplit(){
		String s="a";
		String[] b = s.split(",");
		for(String c:b){
			System.out.println(c);
		}
	}
	@Test
	public void testPattern(){
		
		Pattern pattern = Pattern.compile("\\d");
		Matcher m = pattern.matcher("re2qu11est");
		Matcher m1 = pattern.matcher("re2qu11est");
		StringBuffer sb =new StringBuffer();
		while(m.find()){
			m.appendReplacement(sb, "AA");
//			System.out.println(m.group());
		}
		m.appendTail(sb);
		System.out.println(sb.toString());
		System.out.println(m1.replaceAll("AA"));
	}

	@Test
	public void testEncode(){
		String url="http%3A%2F%2Fwww.baidu.com%2Fa%3Fb%3Dc%26d%3D%E4%B8%AD%E6%96%87";
		try{
			System.out.println(URLDecoder.decode(url,"GBK"));
			System.out.println("aaabc".indexOf("aaa"));
		}catch(Exception e){

		}

	Map<String,Object> map1 = new HashMap<String,Object>();
		map1.put("1","1");
		map1.put("2","2");
		Map<String,Object> map2 = new HashMap<String,Object>();
		map2.put("11","11");
		map1.put("3",map2);

		JsonBinder jsonBinder = JsonBinder.buildNormalBinder(false);
		String sb = jsonBinder.toJson(map1);

		Map<String,Object> map3 = jsonBinder.fromJson(sb,Map.class);
		System.out.println(map3.get("1"));
		Map<String,Object> map4=(Map<String,Object>) map1.get("3");
		System.out.println(map4.get("11"));
	}


	public boolean checkEmail(String email){	// 验证邮箱的正则表达式
		//参考:https://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/

		String format = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		if (email.matches(format)){
			return true;// 邮箱名合法，返回true
		}else{
			return false;// 邮箱名不合法，返回false
		}
	}




	@Test
	public void testRunnable(){
		List<String> features = Arrays.asList("Lambdas", "Default Method", "Stream API", "Date and Time API");
//		features.forEach(e );
		List<String> abc=features.stream().map(e -> e.toUpperCase()).collect(Collectors.toList());
		System.out.println(abc.size());
		abc.forEach(a -> System.out.println(a));
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

	@Test
	public void testTime(){
		String t="2018-03-03";
		String s = getGMT(t);
		System.out.println(s);
	}

	public static void main(String[] args){
			int[] a = new int[]{9,6,70,2,7,11,15};
			int target = 8;
			Arrays.sort(a);
			DESCoderTest dt = new DESCoderTest();
			//int[] b = dt.getSum1(a,target);
		int x =dt.myAtoi(" 045876x");
			System.out.println(x);

	}

	@Test
	public void testArr(){
		int[] a = new int[]{9,6,70,2,7,11,15};
		String[] x= new String[]{"1","3","5"};
		reseta(x);
		for(String b:x){
			System.out.println(b);
		}
	}

	public void reseta(String[] a){
		a[1]="20";

	}

	public int[] getSum(int[] nums,int target){
		Map<Integer,Integer> map=new HashMap<>();
		for(int i=0;i<nums.length;i++){
			Integer index=map.get(target-nums[i]);
			if(index==null){
				map.put(nums[i],i);
			}else{
				return new int[]{i,index};
			}
		}
		return new int[]{0,0};
	}

	public int myAtoi(String str) {
		char[] charArr=str.toCharArray();
		Long result=0L;
		int startIndex=0;
		boolean flag=true;//正数
		int length=0;
		for(int i=0;i<charArr.length;i++){
			if(startIndex==i){
				if(charArr[i]==' '){
					startIndex++;
					continue;
				}
				if(charArr[i]=='+'||charArr[i]=='0'){
					continue;
				}
				if(charArr[i]=='-'){
					flag=false;
					continue;
				}
			}
			if(charArr[i]>='0'&&charArr[i]<='9'){
				System.out.println(charArr[i]);
				System.out.println(charArr[i]+" dddd "+(charArr[i]-'0'));
				result=result*10+charArr[i]-'0';
				length++;
				if(length>10){
					break;
				}
			}else{
				break;
			}
		}
		if(flag){
			if(result>Integer.MAX_VALUE){
				return Integer.MAX_VALUE;
			}
		}else{
			result=-result;
			if(result<Integer.MIN_VALUE){
				return Integer.MIN_VALUE;
			}
		}
		return result.intValue();
	}

	public int[] getSum1(int[] a,int target){
			Map<Integer,Integer> map = new HashMap<Integer, Integer>();
			for(int i=0;i<a.length;i++){
				Integer rs = map.get(target-a[i]);
				if(rs==null){
					map.put(a[i],i);
				}else{
					return new int[]{rs,i};
				}
			}
			return new int[]{0,0};
	}


	@Test
	public void testq(){
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				String urlStr = "http://120.221.66.32/otn/leftTicket/queryZ?leftTicketDTO.train_date=2018-02-25&leftTicketDTO.from_station=SJP&leftTicketDTO.to_station=BJP&purpose_codes=ADULT";
				HttpGet httpget = new HttpGet(urlStr);
				httpget.setHeader("Host", "kyfw.12306.cn");//设置host
				HttpResponse response = httpClient.execute(httpget);
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");
				System.out.println(content);
			}catch (Exception e){
				System.out.println("出錯了");
				e.printStackTrace();

			}


	}


	@Test
	public void testq1(){
		try {

			CloseableHttpClient httpClient = TicketHttpClient.getClient();


			String urlStr = "http://120.221.66.32/otn/leftTicket/queryZ?leftTicketDTO.train_date=2018-02-25&leftTicketDTO.from_station=SJP&leftTicketDTO.to_station=BJP&purpose_codes=ADULT";
			HttpGet httpget = new HttpGet(urlStr);
			httpget.setHeader("Host", "kyfw.12306.cn");//设置host
			HttpResponse response = httpClient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");
			System.out.println(content);
		}catch (Exception e){
			System.out.println("出錯了");
			e.printStackTrace();

		}


	}

}
