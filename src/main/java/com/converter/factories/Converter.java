package com.converter.factories;

import com.converter.factories.state.ConverterState;

import java.util.Map;

/**
 * rtsp流转换器接口
 *
 * @author xufeng
 * @date 2020-05-15
 */
public interface Converter {

	/**
	 * 设置线程状态
	 * @param state 状态标志
	 */
	void setState(ConverterState state);

	/**
	 * 获取该转换的key
	 */
	public String getKey();

	/**
	 * 获取该转换的url
	 *
	 * @return
	 */
	public String getUrl();

	/**
	 * 获取转换的状态
	 *
	 * @return
	 */
	public ConverterState getConverterState();

	/**
	 * 添加一个流输出
	 *
	 * @param entity
	 */
	public void addOutputStreamEntity(String key, OutputStreamEntity entity);

	/**
	 * 所有流输出
	 *
	 * @return
	 */
	public Map<String, OutputStreamEntity> allOutEntity();

	/**
	 * 移除一个流输出
	 *
	 * @param key
	 */
	public void removeOutputStreamEntity(String key);

	/**
	 * 设置修改时间
	 *
	 * @param updateTime
	 */
	public void setUpdateTime(long updateTime);

	/**
	 * 获取修改时间
	 *
	 * @return
	 */
	public long getUpdateTime();

	/**
	 * 退出转换
	 */
	public void exit();

	/**
	 * 启动
	 */
	public void start();

	/**
	 * 获取输出的流
	 *
	 * @param key
	 * @return
	 */
	public OutputStreamEntity getOutputStream(String key);

	/**
	 * 判断线程是否在运行
	 * @return boolean
	 */
	public boolean isRuning();

	/**
	 * 设置运行状态
	 * @param runing 运行标志
	 */
	public void setRuning(boolean runing);
}
