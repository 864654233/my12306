package cn.com.test.my12306.my12306.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Conf {
//	private static Logger logger = Logger.getLogger(Conf.class);
	private static Properties pros = null;
	
	public static void initProperties() {
          InputStream ins = null;
          try {
                  // 生成输入流
                  ins = Conf.class.getResourceAsStream("/config.properties");
                  // 生成properties对象
                  pros = new Properties();
                  pros.load(ins);
                  ins = Conf.class.getResourceAsStream("/sendMail.properties");
                  pros.load(ins);
          } catch (Exception e) {
//          	   logger.error("读取配置文件出错", e);
        	  System.err.println("读取配置文件出错"+e);
                  e.printStackTrace();
          }finally{
          	if(ins != null) {
          		try {
						ins.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
          	}
          }
	}

	public static String getProperties(String key,String defaultValue) {
		if (pros == null) {
			initProperties();
		}

		return pros.getProperty(key, defaultValue);
	}
    public static void main(String args[]) {
    	System.out.println(System.getProperty("user.dir"));
    	System.out.println(Conf.getProperties("redis.maxtotal", "0"));
		System.out.println(Conf.getProperties("redis.maxidle", "0"));
		System.out.println(Conf.getProperties("redis.maxwait", "0"));
		System.out.println(Conf.getProperties("redis.host", "0"));
		System.out.println(Conf.getProperties("redis.port", "0"));
		System.out.println(Conf.getProperties("mongo.host", "0"));
		System.out.println(Conf.getProperties("mongo.port", "0"));
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("结束了");
	}
}

