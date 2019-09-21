package cn.com.test.my12306.my12306.core.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JsUtil {
	public static String escape(String str){
		String rs = "";
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine engine = sem.getEngineByExtension("js");
		try{
			//直接解析
			Object res = engine.eval("escape('"+str+"')");
			rs = res.toString();
//			System.out.println(res);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return rs;
	}

	public static void main(String[] args) {
		String x = JsUtil.escape("昌平北,vvp");
		System.out.println(x);
	}
}