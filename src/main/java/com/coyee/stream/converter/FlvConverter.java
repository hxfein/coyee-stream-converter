package com.coyee.stream.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coyee.stream.exception.ServiceException;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

import com.alibaba.fastjson.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hxfein
 * @className: FlvConverter
 * @description: 将流转为rtmp格式
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
public class FlvConverter extends Converter{
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
     * 流输出
     */
    private List<AsyncContext> outEntitys=new ArrayList<>();


    @Override
    public void run() {
        try {
            log.info("开始转换FLV任务:{}。", endpoint);
            grabber = new FFmpegFrameGrabber(endpoint);
            if ("rtsp".equals(endpoint.substring(0, 4))) {
                grabber.setOption("rtsp_transport", "tcp");
                grabber.setOption("stimeout", "5000000");
            }
            grabber.start();
            if (avcodec.AV_CODEC_ID_H264 == grabber.getVideoCodec()
                    && (grabber.getAudioChannels() == 0 || avcodec.AV_CODEC_ID_AAC == grabber.getAudioCodec())) {
                simpleTransFlv();
            } else {
                transFlv();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeConverter();
            completeResponse();
            log.info("FLV转换任务退出:{}", endpoint);
        }
    }

    private void transFlv() throws FrameRecorder.Exception, FrameGrabber.Exception, InterruptedException {
        log.info("FLV(complex)转换任务启动,可以立即播放：{}。", endpoint);
        grabber.setFrameRate(25);
        if (grabber.getImageWidth() > 1920) {
            grabber.setImageWidth(1920);
        }
        if (grabber.getImageHeight() > 1080) {
            grabber.setImageHeight(1080);
        }
        stream = new ByteArrayOutputStream();
        recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(),
                grabber.getAudioChannels());
        recorder.setInterleaved(true);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("crf", "25");
        recorder.setGopSize(50);
        recorder.setFrameRate(25);
        recorder.setSampleRate(grabber.getSampleRate());
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        }
        recorder.setFormat("flv");
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.start();
        if (headers == null) {
            headers = stream.toByteArray();
            stream.reset();
            writeResponse(headers);
        }
        int nullNumber = 0;
        while (running) {
            // 抓取一帧
            Frame f = grabber.grab();
            if (f != null) {
                try {
                    // 转码
                    recorder.record(f);
                } catch (Exception e) {
                }
                if (stream.size() > 0) {
                    byte[] b = stream.toByteArray();
                    stream.reset();
                    writeResponse(b);
                    if (outEntitys.isEmpty()) {
                        log.info("没有输出退出");
                        break;
                    }
                }
            } else {
                nullNumber++;
                if (nullNumber > 200) {
                    break;
                }
            }
            Thread.sleep(5);
        }
    }

    private void simpleTransFlv() throws FrameRecorder.Exception, FrameGrabber.Exception, InterruptedException {
        // 来源视频H264格式,音频AAC格式
        // 无须转码，更低的资源消耗，更低的延迟
        log.info("FLV(simple)转换任务启动,可以立即播放：{}。", endpoint);
        stream = new ByteArrayOutputStream();
        recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(),
                grabber.getAudioChannels());
        recorder.setInterleaved(true);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("crf", "25");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setSampleRate(grabber.getSampleRate());
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(grabber.getAudioCodec());
        }
        recorder.setFormat("flv");
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.start(grabber.getFormatContext());
        if (headers == null) {
            headers = stream.toByteArray();
            stream.reset();
            writeResponse(headers);
        }
        int nullNumber = 0;
        while (running) {
            AVPacket k = grabber.grabPacket();
            if (k != null) {
                try {
                    recorder.recordPacket(k);
                } catch (Exception e) {
                }
                if (stream.size() > 0) {
                    byte[] b = stream.toByteArray();
                    stream.reset();
                    writeResponse(b);
                    if (outEntitys.isEmpty()) {
                        log.info("没有输出退出");
                        break;
                    }
                }
                avcodec.av_packet_unref(k);
            } else {
                nullNumber++;
                if (nullNumber > 200) {
                    break;
                }
            }
            Thread.sleep(5);
        }
    }

    /**
     * 输出FLV视频流
     *
     * @param b
     */
    public void writeResponse(byte[] b) {
        Iterator<AsyncContext> it = outEntitys.iterator();
        while (it.hasNext()) {
            AsyncContext o = it.next();
            try {
                o.getResponse().getOutputStream().write(b);
            } catch (Exception e) {
                log.info("移除一个输出");
                it.remove();
            }
        }
        if(outEntitys.isEmpty()==false){
            callback.notify(Event.Active,null);
        }
    }

    public void play(HttpServletRequest request,HttpServletResponse response) throws InterruptedException{
        State state=this.getState();
        if(state==State.NEW){
            this.start();
        }else if(state==State.TERMINATED){
            log.info("{}的转流已停止,需要重新开启",endpoint);
            throw new InterruptedException("转流线程已停止,需要重新开启");
        }
        try {
            AsyncContext async = request.startAsync();
            async.setTimeout(0);
            this.addOutputStreamEntity(async);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
        response.setContentType("video/x-flv");
        response.setHeader("Connection", "keep-alive");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            response.flushBuffer();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 退出转换
     */
    public void closeConverter() {
        IOUtils.close(grabber);
        IOUtils.close(recorder);
        IOUtils.close(stream);
    }

    /**
     * 关闭异步响应
     */
    public void completeResponse() {
        Iterator<AsyncContext> it = outEntitys.iterator();
        while (it.hasNext()) {
            AsyncContext o = it.next();
            o.complete();
        }
    }

    public void addOutputStreamEntity(AsyncContext entity) throws IOException {
        if (headers == null) {
            outEntitys.add(entity);
        } else {
            entity.getResponse().getOutputStream().write(headers);
            entity.getResponse().getOutputStream().flush();
            outEntitys.add(entity);
        }
    }

    @Override
    public void softClose() {
        this.running = false;
    }
}
