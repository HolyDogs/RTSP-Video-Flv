package com.converter.registration;

import com.converter.factories.Converter;
import com.converter.factories.ConverterFactories;
import com.converter.factories.state.ConverterState;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FLV流转换
 *
 * @author xufeng
 * @date 2020-05-18
 */
public class ConverterRegistration {

	/**
	 * 转换器集合（根据用户ID分类）
	 */
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Converter>> converters = new ConcurrentHashMap<>();
	/**
	 * 线程池
	 */
	private static ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * 开始一个转换<br/>
	 * 如果已存在这个流的转换就直接返回已存在的转换器
	 * @author xufeng
	 * @param url 视频流链接
	 * @param userId 用户主键
	 * @return converter
	 */
	public static Converter open(String url, String userId) {
		//判断当前用户是否存在转换器线程集合，没有则新建
		ConcurrentHashMap<String, Converter> concurrentHashMap = converters.get(userId);
		if (concurrentHashMap == null) {
			concurrentHashMap = new ConcurrentHashMap<>(16);
			converters.put(userId, concurrentHashMap);
		}
		//判断是否已存在该转换器
		Converter c = isExist(url, concurrentHashMap);
		try {
			if (null == c) {
				String key = UUID.randomUUID().toString();
				//创建线程
				c = new ConverterFactories(url, UUID.randomUUID().toString(), converters.get(userId));
				//记录到集合
				concurrentHashMap.put(key, c);
				//c.start();
				//用线程池启动
				executorService.execute((Runnable) c);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		//如果该线程存在，但处于停止状态，则重新设置状态播放
		if (!c.isRuning()) {
			//设置运行状态
			c.setRuning(true);
			//设置初始化标志
			c.setState(ConverterState.INITIAL);
			//线程池启动
			executorService.execute((Runnable) c);
		}
		return c;
	}

	/**
	 * 如果流已存在，就共用一个
	 * @author xufeng
	 * @param url 链接
	 * @param concurrentHashMap 转换器集合
	 * @return converter
	 */
	public static Converter isExist(String url, ConcurrentHashMap<String, Converter> concurrentHashMap) {
		//遍历集合，根据url判断是否已存在该流视频
		for (Converter c : concurrentHashMap.values()) {
			if (url.equals(c.getUrl())) {
				return c;
			}
		}
		return null;
	}

	/**
	 * 返回集合中的所有转换器
	 * @author xufeng
	 * @param userId 用户主键
	 * @return converters
	 */
	public static ConcurrentHashMap<String, Converter> getAllConverters(String userId){
		return converters.get(userId);
	}
}
