package com.coyee.stream.converter;

import com.alibaba.fastjson.util.IOUtils;
import com.coyee.stream.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;

/**
 * @author hxfein
 * @className: HlsConverter
 * @description: 将流转为hls格式
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
public class HlsConverter extends Converter {
    /**
     * 读流器
     */
    private FFmpegFrameGrabber grabber;
    /**
     * 转码器
     */
    private FFmpegFrameRecorder recorder;
    /**
     * 初始化时转换的秒数
     */
    private static final int MAX_WAIT_COUNT = 10;
    /**
     * 等待队列
     */
    private ArrayBlockingQueue transferWaitQueue = new ArrayBlockingQueue(MAX_WAIT_COUNT);

    @Override
    public void run() {
        try {
            this.running = true;
            log.info("开始转换HLS任务:{}。", endpoint);
            grabber = new FFmpegFrameGrabber(endpoint);
            if ("rtsp".equals(endpoint.substring(0, 4))) {
                grabber.setOption("rtsp_transport", "tcp");
                grabber.setOption("stimeout", "5000000");
            }
            grabber.start();

            int bitrate = grabber.getVideoBitrate();// 比特率
            double framerate = 25.0;// 帧率
            int timebase;// 时钟基
            long err_index = 0, no_pkt_index = 0, total_index = 0;//错误帧数、没有包的帧数、总帧数
            long dts = 0, pts = 0;// pkt的dts、pts时间戳
            // 异常的framerate，强制使用25帧
            if (grabber.getFrameRate() > 0 && grabber.getFrameRate() < 100) {
                framerate = grabber.getFrameRate();
            }

            File m3u8File = this.getIndexFile();
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
            int hlsTime = streamServerConfig.getHlsTime();
            recorder.setOption("hls_time", String.valueOf(hlsTime));
            // HLS播放的列表长度，0标识不做限制
            int hlsListSize = streamServerConfig.getHlsListSize();
            recorder.setOption("hls_list_size", String.valueOf(hlsListSize));
            // TS文件数量限制
            int hlsWrap = streamServerConfig.getHlsWrap();
            recorder.setOption("hls_wrap", String.valueOf(hlsWrap));
            // 设置切片的ts文件序号起始值，默认从0开始，可以通过此项更改
            recorder.setOption("start_number", "100");
            // 每个TS文件的帧数(额外加一帧容错)
            int frame_per_ts = (int) (framerate + 1) * (hlsTime + 1);
            /////开始转码
            AVFormatContext fc = grabber.getFormatContext();
            recorder.start(fc);
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
                boolean recordOk = recorder.recordPacket(pkt);
                err_index += recordOk ? 0 : 1;
                total_index += recordOk ? 1 : 0;
                // pts,dts累加
                timebase = grabber.getFormatContext().streams(pkt.stream_index()).time_base().den();
                pts += (timebase / (int) framerate);
                dts += (timebase / (int) framerate);
                if (total_index % frame_per_ts == 0) {
                    int current_size = transferWaitQueue.size();
                    if (current_size > MAX_WAIT_COUNT) {
                        transferWaitQueue.clear();
                    }
                    transferWaitQueue.put(total_index);
                }

            }
            log.info("{}转换完成,总帧数为{},错误帧数为{},空帧数为{}。", endpoint, total_index, err_index, no_pkt_index);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeConverter();
        }
    }

    /**
     * 获取m3u8文件存储地址
     *
     * @return
     */
    public File getIndexFile() {
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
     * 获取视频片段文件
     *
     * @param filename
     * @return
     */
    public File getTsFile(String filename) {
        String hlsStoreDir = streamServerConfig.getHlsStoreDir();
        String tsPath = FilenameUtils.separatorsToSystem(hlsStoreDir + File.separator + key + File.separator + filename + ".ts");
        File tsFile = new File(tsPath);
        File tsParentFile = tsFile.getParentFile();
        if (tsParentFile.exists() == false) {
            tsParentFile.mkdirs();
        }
        return tsFile;
    }

    /**
     * 退出转换
     */
    public void closeConverter() {
        IOUtils.close(grabber);
        IOUtils.close(recorder);
    }


    @Override
    public void softClose() {
        this.running = false;
    }

    /***
     * 启动转换线程并等待转换为可播放状态
     */
    public void startAndWait() {
        try {
            long begMills=System.currentTimeMillis();
            this.start();
            File indexFile = this.getIndexFile();
            long currentTimeMillis = System.currentTimeMillis();
            long lastModified = indexFile.lastModified();
            long m3u8ExpireMills = streamServerConfig.getM3u8ExpireMills();
            if (Math.abs(currentTimeMillis - lastModified) > m3u8ExpireMills) {
                FileUtils.deleteDirectory(indexFile.getParentFile());
                log.info("索引文件已过期准备删除:{}", indexFile.getParentFile());
            }
            while (indexFile.exists() == false) {
                transferWaitQueue.take();
            }
            long endMills=System.currentTimeMillis();
            log.info("转换流【{}】共耗时 {}",endpoint,(endMills-begMills));
        } catch (Exception er) {
            throw new ServiceException("等待流转换失败", er);
        }
    }
}
