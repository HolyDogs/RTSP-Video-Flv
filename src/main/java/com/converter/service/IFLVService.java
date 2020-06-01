package com.converter.service;


/**
 * 视频流转换接口
 * @author xufeng
 */
public interface IFLVService {

	/**
	 * 打开一个流地址
	 *
	 * @param url rtsp流链接
	 * @param userId 用户主键
	 * @param response 响应请求
	 * @author xufeng
	 */
	void open(String url,String userId, Object response);

}
