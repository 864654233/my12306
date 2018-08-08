package cn.com.test.my12306.my12306.core.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * 
 * 例子：<br/>
 * private static JsonBinder binder = JsonBinder.buildNonDefaultBinder();<br/>
 * binder.setDateFormat("yyyy-MM-dd");<br/>
 * <h1>注意：在于对象互转时，对象一定要有无参构造函数</h1>
 * 
 * @author
 * 
 */
public class JsonBinder {
	private Logger logger = LogManager.getLogger(getClass());

	private ObjectMapper mapper;

	public JsonBinder(JsonInclude.Include inclusion) {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(inclusion);
		mapper.enableDefaultTyping();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		// 忽略无对应字段的情况
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
	public JsonBinder(JsonInclude.Include inclusion,boolean isTyping) {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(inclusion);
		if(isTyping){
			mapper.enableDefaultTyping();
		}else{
			mapper.disableDefaultTyping();
		}
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		// 忽略无对应字段的情况
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	/**
	 * 创建输出全部属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNormalBinder() {
		return new JsonBinder(JsonInclude.Include.ALWAYS);
	}
	/**
	 * 创建输出全部属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNormalBinder(boolean isTyping) {
		return new JsonBinder(JsonInclude.Include.ALWAYS,isTyping);
	}

	/**
	 * 创建只输出非空属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNonNullBinder() {
		return new JsonBinder(JsonInclude.Include.NON_NULL);
	}
	/**
	 * 创建只输出非空属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNonNullBinder(boolean isTyping) {
		return new JsonBinder(JsonInclude.Include.NON_NULL,isTyping);
	}

	/**
	 * 创建只输出初始值被改变的属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNonDefaultBinder() {
		return new JsonBinder(JsonInclude.Include.NON_DEFAULT);
	}
	
	/**
	 * 创建只输出初始值被改变的属性到Json字符串的Binder.
	 */
	public static JsonBinder buildNonDefaultBinder(boolean isTyping) {
		return new JsonBinder(JsonInclude.Include.NON_DEFAULT,isTyping);
	}

	/**
	 * 如果JSON字符串为Null或"null"字符串,返回Null. 如果JSON字符串为"[]",返回空集合.
	 * 
	 * 如需读取集合如List/Map,且不是List&lt;String&gt;这种简单类型时使用如下语句: List&lt;MyBean&gt; beanList =
	 * binder.getMapper().readValue(listString, new
	 * TypeReference&lt;List&lt;MyBean&gt;&gt;() {});
	 */
	public <T> T fromJson(String jsonString, Class<T> clazz) {
		if (jsonString == null || jsonString.isEmpty()) {
			return null;
		}

		try {
			return mapper.readValue(jsonString, clazz);
		} catch (IOException e) {
			logger.warn("parse json string error:" + jsonString, e);
			throw new RuntimeException("JSON格式错误");
		}
	}
	
	/**
	 * 支持带泛型的JSON类型转换
	 * @param jsonString
	 * @param clazz
	 * @param javaType
	 * @since 1.1.6
	 * @author yaming.xu
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T fromJson(String jsonString, Class<T> clazz,JavaType javaType)throws Exception{
		if (jsonString == null || jsonString.isEmpty()) {
			return null;
		}

		try {
			return (T)mapper.readValue(jsonString, javaType);
		} catch (IOException e) {
			logger.warn("parse json string error:" + jsonString+e.getMessage());
			throw e;
		}
	}
	/**
	 * 生成泛型包装类
	 * @param collectionClass
	 * @param elementClasses
	 * @author yaming.xu
	 * @since 1.1.6
	 * @return
	 */
	public JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses){
		return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);   
	}

	/**
	 * 如果对象为Null,返回"null". 如果集合为空集合,返回"[]".
	 */
	public String toJson(Object object) {

		try {
			return mapper.writeValueAsString(object);
		} catch (IOException e) {
			logger.warn("write to json string error:" + object, e);
			return null;
		}
	}

	/**
	 * 设置转换日期类型的format pattern,如果不设置默认打印Timestamp毫秒数.
	 */
	public void setDateFormat(String pattern) {
		if (pattern != null && !pattern.isEmpty()) {
			DateFormat df = new SimpleDateFormat(pattern);
			mapper.setDateFormat(df);
		}
	}

	/**
	 * 取出Mapper做进一步的设置或使用其他序列化API.
	 */
	public ObjectMapper getMapper() {
		return mapper;
	}
}
