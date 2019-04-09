package cn.com.test.my12306.my12306.core.util;

import cn.com.test.my12306.my12306.core.TicketHttpClient;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class ImageUtil {
	static BASE64Encoder encoder = new sun.misc.BASE64Encoder();
	static BASE64Decoder decoder = new sun.misc.BASE64Decoder();
	/**
	 * //将图片文件转化为字节数组字符串，并对其进行Base64编码处理
	 * @param imgFile
	 * @return
	 */
	public static String GetImageStr(String imgFile){
		InputStream in = null;
		byte[] data = null;
		//读取图片字节数组
		try{
			in = new FileInputStream(imgFile);
			data = new byte[in.available()];
			in.read(data);
			in.close();
		}catch (IOException e){
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		//对字节数组Base64编码
		BASE64Encoder encoder = new BASE64Encoder();
		return encoder.encode(data);//返回Base64编码过的字节数组字符串
	}

	/**
	 * 对字节数组字符串进行Base64解码并生成图片
	 * @param base64str
	 * @param savepath
	 * @return
	 */
	public static boolean GenerateImage(String base64str,String savepath){
		if (base64str == null) //图像数据为空
			return false;
		// System.out.println("开始解码");
		BASE64Decoder decoder = new BASE64Decoder();
		try{
			//Base64解码
			byte[] b = decoder.decodeBuffer(base64str);
			//  System.out.println("解码完成");
			for(int i=0;i<b.length;++i){
				if(b[i]<0){//调整异常数据
					b[i]+=256;
				}
			}
			// System.out.println("开始生成图片");
			//生成jpeg图片
			OutputStream out = new FileOutputStream(savepath);
			out.write(b);
			out.flush();
			out.close();
			return true;
		}catch (Exception e){
			return false;
		}
	}

	/**
	 * 将图片转换成二进制
	 * @return
	 */
	public static String getImageBinary(String imgFile){
		File f = new File(imgFile);
		BufferedImage bi;
		try {
			bi = ImageIO.read(f);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "jpg", baos);
			byte[] bytes = baos.toByteArray();

			return encoder.encodeBuffer(bytes).trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static byte[] getImageBinary1(String imgFile){
		File f = new File(imgFile);
		BufferedImage bi;
		try {
			bi = ImageIO.read(f);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "jpg", baos);
			byte[] bytes = baos.toByteArray();

			return bytes;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 将二进制转换为图片
	 * @param base64String
	 */
	public static void base64StringToImage(String base64String){
		try {
			byte[] bytes1 = decoder.decodeBuffer(base64String);

			ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
			BufferedImage bi1 =ImageIO.read(bais);
			File w2 = new File("e://QQ.jpg");//可以是jpg,png,gif格式
			ImageIO.write(bi1, "jpg", w2);//不管输出什么格式图片，此处不需改动
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void download(String urlString, String filename,String savePath) throws Exception {
		// 构造URL
		URL url = new URL(urlString);
		// 打开连接
		URLConnection con = url.openConnection();
		//设置请求超时为5s
		con.setConnectTimeout(5*1000);
		// 输入流
		InputStream is = con.getInputStream();
		// 1K的数据缓冲
		byte[] bs = new byte[1024];
		// 读取到的数据长度
		int len;
		// 输出的文件流
		File sf=new File(savePath);
		if(!sf.exists()){
			sf.mkdirs();
		}
		OutputStream os = new FileOutputStream(sf.getPath()+"\\"+filename);
		// 开始读取
		while ((len = is.read(bs)) != -1) {
			os.write(bs, 0, len);
		}
		// 完毕，关闭所有链接
		os.close();
		is.close();
	}



	public static void  main1(String[] args){
		String base64 = ImageUtil.GetImageStr("F:\\临时\\pic\\1.jpg");
		CloseableHttpResponse response=null;
		JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
		BasicCookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.build();
		try {
			/*HttpUriRequest checkCode = RequestBuilder.post()
					.setUri(new URI("http://ec2-18-191-165-87.us-east-2.compute.amazonaws.com:8002"))
					.addParameter("name", "hello_123")
					.addParameter("password", "hello_123")
					.addParameter("imgData", base64)
					.build();*/
			HttpUriRequest checkCode = RequestBuilder.post()
					.setUri(new URI("http://123.57.138.40:9443/12306/code"))
					.addParameter("user", "111")
					.addParameter("key", "123456")
//					.addParameter("file", base64)
					.addParameter("file", ImageUtil.getImageBinary("F:\\临时\\pic\\1.jpg"))
					.build();
			response = httpclient.execute(checkCode);

			Map<String, Object> rsmap = null;

			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);
			rsmap = jsonBinder.fromJson(responseBody, Map.class);
			System.out.println(rsmap);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 识别图片验证码
	 * @param savePath 存放路径
	 * @param imageName 图片名称
	 * @return
	 */
	public static String shibie(String savePath,String imageName){
		String rs ="";
		try {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpPost uploadFile = new HttpPost("http://123.57.138.40:9443/12306/code");
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//			builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);
			ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
			builder.addTextBody("user", "111", contentType);
			builder.addTextBody("key", "123456", contentType);
			// 把文件加到HTTP的post请求中
//			File f = new File("F:\\临时\\pic\\1.jpg");
			File f = new File(savePath+File.separatorChar+imageName);
			builder.addBinaryBody("file",new FileInputStream(f),ContentType.APPLICATION_OCTET_STREAM,f.getName());
			HttpEntity multipart = builder.build();
			uploadFile.setEntity(multipart);
			CloseableHttpResponse response = httpClient.execute(uploadFile);
			HttpEntity responseEntity = response.getEntity();
			String sResponse=EntityUtils.toString(responseEntity, "UTF-8");
			System.out.println("Post 返回结果"+sResponse);
			rs = sResponse;
		}catch (Exception e){
			e.printStackTrace();
		}
		return rs;
	}

	/**
	 * 识别图片验证码
	 * @param imageBase64 图片的base64
	 * @return 识别成功的验证码 可以直接用于校验
	 */
	public static String shibie360(String imageBase64){
		JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
		String rs ="";
		try {

//			String token ="";
			String check ="";
//			imageBase64="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";
			StringEntity stringEntity = new StringEntity("{\"base64\":\""+imageBase64+"\"}", "application/json", "utf-8");
			HttpUriRequest checkCode = RequestBuilder.post()
//					.setUri(new URI("http://60.205.200.159/api")).setEntity(stringEntity)
					.setUri(new URI("https://12306.jiedanba.cn/api/v2/getCheck")).setEntity(stringEntity)
//					.addParameter("base64", imageBase64)
					.build();
			CloseableHttpClient httpClient= TicketHttpClient.getClient();
			CloseableHttpResponse response = httpClient.execute(checkCode);
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");
			System.out.println(content);
			Map<String, Object> rsmap = jsonBinder.fromJson(content, Map.class);
			if(String.valueOf(rsmap.get("success")).equals("true")){
//				token =String.valueOf(rsmap.get("token"));
				Map<String,String> dataMap = (Map<String,String>)rsmap.get("data");
				check = String.valueOf(dataMap.get("check"));
			}

			Header header0 = new BasicHeader("Host","check.huochepiao.360.cn");
			Header header1 = new BasicHeader("Upgrade-Insecure-Requests","1");
			Header header2 = new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36 QIHU 360EE");
//			Header header3 = new BasicHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
			Header header3 = new BasicHeader("Content-Type","application/json;charset=UTF-8");
//			Header header3 = new BasicHeader("Content-Type","text/plain");
			int a=1;
			stringEntity = new StringEntity("{\"=\":\"\",\"check\":\""+check+"\",\"img_buf\":\""+imageBase64+"\",\"logon\":"+a+",\"type\":\"D\"}","utf-8");
			stringEntity.setContentType("application/json;charset=UTF-8");
//			stringEntity.setContentEncoding("UTF-8");
			HttpUriRequest getVcode = RequestBuilder.post()
					.setUri(new URI("https://check.huochepiao.360.cn/img_vcode"))
//					.setUri(new URI("https://12306.jiedanba.cn/api/v2/img_vcode"))
					.setEntity(stringEntity)
					.addHeader(header0).addHeader(header1).addHeader(header2).addHeader(header3)
					.build();
			CloseableHttpResponse response1 = httpClient.execute(getVcode);
			HttpEntity entity1 = response1.getEntity();
			String content1 = EntityUtils.toString(entity1, "UTF-8");
			System.out.println(content1);
			Map<String, Object> rsmap1 = jsonBinder.fromJson(content1, Map.class);
			Object res = rsmap1.get("res");

			if(!StringUtils.isEmpty(res)){
				rs = res.toString().replaceAll("\\),\\(",",").replaceAll("\\)","").replaceAll("\\(","");
			}

		}catch (Exception e){
			e.printStackTrace();
		}
		return rs;
	}

	/**
	 * 将验证码结果转换成坐标
	 * @param resStr
	 * @return
	 */
	public static String getZuobiao(String resStr){
		JsonBinder jsonBinder = JsonBinder.buildNonNullBinder(false);
		Map<String, Object> rsmap = null;
		String zuobiao ="";
		try {
			int baseX = 10,baseY=35;
			//{"time": "6.638509s", "result": [2, 6], "msg": "success", "code": 0, "text": ["雨靴"]}
			rsmap = jsonBinder.fromJson(resStr, Map.class);

			if(null!=rsmap && (rsmap.get("code")+"").equals("0")){
				String zb = rsmap.get("result")+"";
				if(!"".equals(zb) && zb.contains("[")){
					zb = zb.replace("[","").replace("]","").replaceAll(" ","");
					String[] zbArr = zb.split(",");
					for(String z:zbArr){
						int x1= (int) (Math.random() * 5 + 1);
						int y1= (int) (Math.random() * 5 + 1);
						int z1=Integer.parseInt(z);
						zuobiao += baseX+(z1%4==0?4-1:z1%4-1)*70+x1+",";
						zuobiao+= baseY+ Integer.parseInt(z)/5*70+y1;
						zuobiao+=",";
					}

				}
			}
			if(zuobiao.endsWith(",")){
				zuobiao=zuobiao.substring(0,zuobiao.length()-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return zuobiao;
	}

	public static String test2(){
		String rs ="";
		try {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpPost uploadFile = new HttpPost("http://123.57.138.40:9443/12306/code");
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//			builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);
			ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
			builder.addTextBody("name", "hello_123", contentType);
			builder.addTextBody("password", "hello_123", contentType);
			builder.addTextBody("imgData", "hello_123", contentType);
			// 把文件加到HTTP的post请求中
//			File f = new File("F:\\临时\\pic\\1.jpg");
//			builder.addBinaryBody("file",new FileInputStream(f),ContentType.APPLICATION_OCTET_STREAM,f.getName());
//			HttpEntity multipart = builder.build();
//			uploadFile.setEntity(multipart);
			CloseableHttpResponse response = httpClient.execute(uploadFile);
			HttpEntity responseEntity = response.getEntity();
			String sResponse=EntityUtils.toString(responseEntity, "UTF-8");
			System.out.println("Post 返回结果"+sResponse);
			rs = sResponse;

		}catch (Exception e){
			e.printStackTrace();
		}
		return rs;
	}

	public static void  main(String[] args){
//		ImageUtil.shibie("F:\\临时\\pic","1.jpg");
//		String abc="a+1+b++c";
//		System.out.println(abc.replaceAll("\\+"," "));
		ImageUtil.shibie360("");
	}


}