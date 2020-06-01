package com.converter.factories;

import com.converter.factories.state.ConverterState;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * javacv转包装<br/>
 * 无须转码，更低的资源消耗，更低的延迟<br/>
 * 确保流来源视频H264格式,音频AAC格式
 *
 * @author xufeng
 * @date 2020-05-15
 */
public class ConverterFactories extends Thread implements Converter {

	/**
	 * 运行状态
	 */
	public volatile boolean runing = true;
	/**
	 * 读流器
	 */
	private FFmpegFrameGrabber grabber;
	/**
	 * 转码器
	 */
	private FFmpegFrameRecorder recorder;
	/**
	 * 转FLV格式的头信息<br/>
	 * 如果有第二个客户端播放首先要返回头信息
	 */
	private byte[] headers;
	/**
	 * 保存转换好的流
	 */
	private ByteArrayOutputStream stream;
	/**
	 * 流地址，h264,aac
	 */
	private String url;
	/**
	 * 流输出
	 */
	private Map<String, OutputStreamEntity> outEntitys;

	/**
	 * 当前转换器状态
	 */
	private ConverterState state = ConverterState.INITIAL;
	/**
	 * key用于表示这个转换器
	 */
	private String key;
	/**
	 * 上次更新时间<br/>
	 * 客户端读取是刷新<br/>
	 * 如果没有客户端读取，会在一分钟后销毁这个转换器
	 */
	private long updateTime;
	/**
	 * 转换队列
	 */
	private Map<String, Converter> factories;

	public ConverterFactories(String url, String key, Map<String, Converter> factories) {
		this.url = url;
		this.key = key;
		this.factories = factories;
		this.updateTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		try {
			//使用ffmpeg抓取流，创建读流器
			grabber = new FFmpegFrameGrabber(url);
			//如果为rtsp流，增加配置
			if ("rtsp".equals(url.substring(0, 4))) {
				//设置打开协议tcp / udp
				grabber.setOption("rtsp_transport", "tcp");
				//设置未响应超时时间 0.5秒
				grabber.setOption("stimeout", "500000");
				//设置缓存大小，提高画质、减少卡顿花屏
				//grabber.setOption("buffer_size", "1024000");
				//设置视频比例
				//grabber.setAspectRatio(1.7777);
			} else {
				grabber.setOption("timeout", "500000");
			}
			grabber.start();
			stream = new ByteArrayOutputStream();
			outEntitys = new ConcurrentHashMap<>();
			//设置转换状态为打开
			state = ConverterState.OPEN;
			//创建转码器
			recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(),
					grabber.getAudioChannels());
			//配置转码器
			recorder.setFrameRate(grabber.getFrameRate());
			recorder.setSampleRate(grabber.getSampleRate());
			if (grabber.getAudioChannels() > 0) {
				recorder.setAudioChannels(grabber.getAudioChannels());
				recorder.setAudioBitrate(grabber.getAudioBitrate());
				recorder.setAudioCodec(grabber.getAudioCodec());
				//设置视频比例
				//recorder.setAspectRatio(grabber.getAspectRatio());
			}
			recorder.setFormat("flv");
			recorder.setVideoBitrate(grabber.getVideoBitrate());
			recorder.setVideoCodec(grabber.getVideoCodec());
			recorder.start(grabber.getFormatContext());
			//进入写入运行状态
			state = ConverterState.RUN;
			if (headers == null) {
				headers = stream.toByteArray();
				stream.reset();
				for (OutputStreamEntity o : outEntitys.values()) {
					o.getOutput().write(headers);
				}
			}
			int errorNum = 0;
			//线程运行时
			while (runing) {
				//FFmpeg读流压缩
				AVPacket k = grabber.grabPacket();
				if (k != null) {
					try {
						//转换器转换
						recorder.recordPacket(k);
					} catch (Exception e) {
					}
					byte[] b = stream.toByteArray();
					stream.reset();
					for (OutputStreamEntity o : outEntitys.values()) {
						if (o.getOutput().size() < (1024 * 1024)) {
							//写出
							o.getOutput().write(b);
						}
					}
					errorNum = 0;
				} else {
					errorNum++;
					if (errorNum > 500) {
						break;
					}
				}
			}
		} catch (Exception e) {
			//log.error(e.getMessage(), e);
			state = ConverterState.ERROR;
		} finally {
			closeConverter();
			//log.info("exit");
			state = ConverterState.CLOSE;
			factories.remove(this.key);
		}
	}

	/**
	 * 退出转换
	 */
	public void closeConverter() {
		try {
			//停止转码器
			if (null != recorder) {
				recorder.stop();
			}
			//停止、关闭读流器
			grabber.stop();
			grabber.close();
			//关闭转码器
			if (null != recorder) {
				recorder.close();
			}
			//关闭流
			if (null != stream) {
				stream.close();
			}
			if (null != outEntitys) {
				for (OutputStreamEntity o : outEntitys.values()) {
					o.getOutput().close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			//log.error(e.getMessage(), e);
		}
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String getUrl() {
		return this.url;
	}

	@Override
	public ConverterState getConverterState() {
		return this.state;
	}

	@Override
	public void addOutputStreamEntity(String key, OutputStreamEntity entity) {
		try {
			switch (this.state) {
				case INITIAL:
					Thread.sleep(100);
					addOutputStreamEntity(key, entity);
					break;
				case OPEN:
					outEntitys.put(key, entity);
					break;
				case RUN:
					entity.getOutput().write(this.headers);
					outEntitys.put(key, entity);
					break;
				default:
					break;
			}
		} catch (Exception e) {
			//log.error(e.getMessage(), e);
		}
	}

	@Override
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public long getUpdateTime() {
		return this.updateTime;
	}

	@Override
	public void exit() {
		//设置线程状态为非运行状态，最后会进入finally块关闭读流器、转码器、流
		this.runing = false;
		try {
			this.join();
		} catch (Exception e) {
			e.printStackTrace();
			//log.error(e.getMessage(), e);
		}
	}

	@Override
	public OutputStreamEntity getOutputStream(String key) {
		if (outEntitys.containsKey(key)) {
			return outEntitys.get(key);
		}
		return null;
	}

	@Override
	public Map<String, OutputStreamEntity> allOutEntity() {
		return this.outEntitys;
	}

	@Override
	public void removeOutputStreamEntity(String key) {
		this.outEntitys.remove(key);
	}

	@Override
	public boolean isRuning() {
		return runing;
	}

	@Override
	public void setRuning(boolean runing) {
		this.runing = runing;
	}

	@Override
	public void setState(ConverterState state) {
		this.state = state;
	}
}
