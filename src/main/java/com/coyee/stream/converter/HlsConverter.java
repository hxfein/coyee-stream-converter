package com.coyee.stream.converter;

import com.alibaba.fastjson.util.IOUtils;
import com.coyee.stream.config.StreamServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import javax.servlet.AsyncContext;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;

/**
 * @author hxfein
 * @className: HlsConverter
 * @description: 将流转为hls格式
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
public class HlsConverter extends Thread implements Converter {
    public volatile boolean running = true;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    /**
     * 读流器
     */
    private FFmpegFrameGrabber grabber;
    /**
     * 转码器
     */
    private FFmpegFrameRecorder recorder;
    /**
     * 流服务配置
     */
    private StreamServerConfig streamServerConfig;

    /**
     * 流地址，h264,aac
     */
    private String url;

    private String key;


    public HlsConverter(StreamServerConfig config, String url, String key) {
        this.streamServerConfig = config;
        this.url = url;
        this.key = key;
    }

    @Override
    public void run() {
        try {
            lock.lock();
            log.info("开始转换HLS任务:{}。", url);
            grabber = new FFmpegFrameGrabber(url);
            if ("rtsp".equals(url.substring(0, 4))) {
                grabber.setOption("rtsp_transport", "tcp");
                grabber.setOption("stimeout", "5000000");
            }
            grabber.start();

            int bitrate = grabber.getVideoBitrate();// 比特率
            double framerate = 25.0;// 帧率
            int timebase;// 时钟基
            int err_index = 0, no_pkt_index = 0;//错误帧数、没有包的帧数
            long dts = 0, pts = 0;// pkt的dts、pts时间戳
            // 异常的framerate，强制使用25帧
            if (grabber.getFrameRate() > 0 && grabber.getFrameRate() < 100) {
                framerate = grabber.getFrameRate();
            }

            File m3u8File = this.getM3u8File();
            recorder = new FFmpegFrameRecorder(m3u8File, grabber.getImageWidth(), grabber.getImageHeight(),
                    grabber.getAudioChannels());
            // 设置比特率
            recorder.setVideoBitrate(bitrate);
            // h264编/解码器
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            // 设置音频编码
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏)
            recorder.setFrameRate(framerate);
            // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
            recorder.setGopSize((int) framerate);
            // 解码器格式
            recorder.setFormat("hls");
            // 单个切片时长,单位是s，默认为5s
            int hlsTime=streamServerConfig.getHlsTime();
            recorder.setOption("hls_time", String.valueOf(hlsTime));
            // HLS播放的列表长度，0标识不做限制
            int hlsListSize=streamServerConfig.getHlsListSize();
            recorder.setOption("hls_list_size", String.valueOf(hlsListSize));
            // TS文件数量限制
            int hlsWrap=streamServerConfig.getHlsWrap();
            recorder.setOption("hls_wrap", String.valueOf(hlsWrap));
            // 设置切片的ts文件序号起始值，默认从0开始，可以通过此项更改
            recorder.setOption("start_number", "100");
            /////开始转码
            AVFormatContext fc = grabber.getFormatContext();
            recorder.start(fc);
            boolean canPlay = false;
            while (running) {
                AVPacket pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
                    Thread.sleep(1);
                    no_pkt_index++;
                    continue;
                }
                // 获取到的pkt的dts，pts异常，将此包丢弃掉。
                if (pkt.dts() == avutil.AV_NOPTS_VALUE && pkt.pts() == avutil.AV_NOPTS_VALUE || pkt.pts() < dts) {
                    av_packet_unref(pkt);
                    continue;
                }
                // 矫正dts，pts
                pkt.pts(pts);
                pkt.dts(dts);
                err_index += (recorder.recordPacket(pkt) ? 0 : 1);
                // pts,dts累加
                timebase = grabber.getFormatContext().streams(pkt.stream_index()).time_base().den();

                pts += (timebase / (int) framerate);
                dts += (timebase / (int) framerate);
                if (canPlay == false && m3u8File.exists()) {
                    condition.signal();
                    lock.unlock();
                    canPlay = true;
                    log.info("HLS转换的文件可以播放了：{}。", url);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeConverter();
            log.info("HLS转换{}的任务结束。", url);
        }
    }

    /**
     * 获取m3u8文件存储地址
     *
     * @return
     */
    public File getM3u8File() {
        String hlsStoreDir = streamServerConfig.getHlsStoreDir();
        String hlsUrl = FilenameUtils.separatorsToSystem(hlsStoreDir + File.separator + key + File.separator + "play.m3u8");
        File hlsFile = new File(hlsUrl);
        File hlsParentFile = hlsFile.getParentFile();
        if (hlsParentFile.exists() == false) {
            hlsParentFile.mkdirs();
        }
        return hlsFile;
    }

    /**
     * 退出转换
     */
    public void closeConverter() {
        IOUtils.close(grabber);
        IOUtils.close(recorder);
    }

    @Override
    public void addOutputStreamEntity(String key, AsyncContext entity) throws IOException {

    }

    @Override
    public void softClose() {
        this.running = false;
    }


    public String getPlayUrl() throws InterruptedException {
        lock.lock();
        condition.await(10, TimeUnit.SECONDS);
        lock.unlock();
        return String.format("/live/%s/play.m3u8", key);
    }

}
