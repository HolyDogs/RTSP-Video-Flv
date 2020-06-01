package com.converter.service.impl;

import com.converter.factories.Converter;
import com.converter.factories.OutputStreamEntity;
import com.converter.registration.ConverterRegistration;
import com.converter.service.IFLVService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * FLV流转换
 *
 * @author xufeng
 * @date 2020-05-14下午5:06:46
 */
@Service("flvService")
public class FLVService implements IFLVService {

	/**
	 * 打开一个流地址，写入response
	 * @param url 流地址
	 * @param userId 用户主键
	 * @param object HttpServletResponse
	 * @author xufeng
	 */
	@Override
	public void open(String url, String userId, Object object) {
		//创建转换器线程并启动
		Converter c = ConverterRegistration.open(url, userId);
		//UUID设置一个key值
		String key = UUID.randomUUID().toString();
		//创建输出字节流
		OutputStreamEntity outEntity = new OutputStreamEntity(new ByteArrayOutputStream(), System.currentTimeMillis(),
				key);
		//添加流输出
		c.addOutputStreamEntity(key, outEntity);
		try {
			HttpServletResponse response = (HttpServletResponse) object;
			//设置响应头
			response.setContentType("video/x-flv");
			response.setHeader("Connection", "keep-alive");
			response.setStatus(HttpServletResponse.SC_OK);
			//写出缓冲信息，并清空
			response.flushBuffer();
			//循环读取outEntity里的流输出给前台
			readFlvStream(c, outEntity, response);
		} catch (Exception e) {
			//客户端长连接过程中被异常关闭，关闭该长连接对应的转换器线程
			c.exit();
			e.printStackTrace();
			//c.removeOutputStreamEntity(outEntity.getKey());
		}
	}

	/**
	 * 递归读取转换好的视频流
	 *
	 * @param c 转换器
	 * @param outEntity 输出流
	 * @param response 响应
	 * @author xufeng
	 * @throws Exception
	 */
	public void readFlvStream(Converter c, OutputStreamEntity outEntity, HttpServletResponse response)
			throws Exception {
		//根据转换器状态来决定是继续等待、读取、结束流输出
		switch (c.getConverterState()) {
			case INITIAL:
				Thread.sleep(300);
				readFlvStream(c, outEntity, response);
				break;
			case OPEN:
				Thread.sleep(100);
				readFlvStream(c, outEntity, response);
				break;
			case RUN:
				if (outEntity.getOutput().size() > 0) {
					byte[] b = outEntity.getOutput().toByteArray();
					outEntity.getOutput().reset();
					response.getOutputStream().write(b);
					outEntity.setUpdateTime(System.currentTimeMillis());
				}
				c.setUpdateTime(System.currentTimeMillis());
				Thread.sleep(100);
				readFlvStream(c, outEntity, response);
				break;
			case CLOSE:
				//log.info("close");
				break;
			default:
				break;
		}
	}

}
