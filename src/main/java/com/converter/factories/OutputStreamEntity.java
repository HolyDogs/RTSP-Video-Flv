package com.converter.factories;

import java.io.ByteArrayOutputStream;

/**
 * 用于输出视频流
 *
 * @author xufeng
 * @date 2020-05-15
 */

public class OutputStreamEntity {

	public OutputStreamEntity(ByteArrayOutputStream output, long updateTime, String key) {
		super();
		this.output = output;
		this.updateTime = updateTime;
		this.key = key;
	}

	/**
	 * 字节数组输出流
	 */
	private ByteArrayOutputStream output;
	/**
	 * 更新时间
	 */
	private long updateTime;
	/**
	 * key标识
	 */
	private String key;

	public ByteArrayOutputStream getOutput() {
		return output;
	}

	public void setOutput(ByteArrayOutputStream output) {
		this.output = output;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}


}
