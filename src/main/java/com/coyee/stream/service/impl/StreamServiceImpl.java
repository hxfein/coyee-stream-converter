package com.coyee.stream.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coyee.stream.config.StreamServerConfig;
import com.coyee.stream.converter.HlsConverter;
import com.coyee.stream.converter.Converter;
import com.coyee.stream.converter.FlvConverter;
import com.coyee.stream.service.IStreamService;
import com.coyee.stream.util.Des;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import lombok.extern.slf4j.Slf4j;

/**
 * @author hxfein
 * @className: StreamServiceImpl
 * @description: 流服务相关接口实现
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
@Service
public class StreamServiceImpl implements IStreamService {

    private Map<String, Converter> flvConverters = new HashMap<>();
    private Map<String, Converter> hlsConverters = new HashMap<>();
    private Map<String, Date> activeStreamMap = new HashMap<>();
    @Resource
    private StreamServerConfig streamServerConfig;
    /**
     * 编码
     */
    private static Charset charset = Charset.forName("utf-8");

    @Override
    public String open(String url, String format, HttpServletResponse response, HttpServletRequest request) {
        String key = encode(url);
        this.activeStream(key);
        //如果是hls协议，开启转换，转换成功以后返回播放地址给客户端
        //如果是flv协议，开启转换，并保持与客户端的链接，不断的输出视频流给客户端
        if (StringUtils.equals(format, "hls")) {
            HlsConverter hlsConverter = null;
            if (hlsConverters.containsKey(key) == false) {
                hlsConverter = new HlsConverter(streamServerConfig, url, key);
                hlsConverter.start();
                hlsConverters.put(key, hlsConverter);
            } else {
                hlsConverter = (HlsConverter) hlsConverters.get(key);
                log.info("流{}的转换任务已存在，可以复用。", url);
            }
            try {
                String playUrl = hlsConverter.getPlayUrl();
                return playUrl;
            } catch (InterruptedException er) {
                throw new RuntimeException("获取播放地址失败！");
            }
        } else {
            AsyncContext async = request.startAsync();
            async.setTimeout(0);
            if (flvConverters.containsKey(key)) {
                Converter c = flvConverters.get(key);
                try {
                    c.addOutputStreamEntity(key, async);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new IllegalArgumentException(e.getMessage());
                }
            } else {
                List<AsyncContext> outs = new ArrayList<>();
                outs.add(async);
                FlvConverter c = new FlvConverter(url, outs);
                c.start();
                flvConverters.put(key, c);
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
        return null;
    }

    @Override
    public boolean activeStream(String key) {
        if (hlsConverters.containsKey(key) || flvConverters.containsKey(key)) {
            activeStreamMap.put(key, new Date());
            return true;
        } else {
            activeStreamMap.remove(key);
            return false;
        }
    }

    @Override
    public String encode(String url) {
        String desKey = streamServerConfig.getDesKey();
        return Des.encryptString(url, charset, desKey);
    }

    @Override
    public String decode(String url) {
        String desKey = streamServerConfig.getDesKey();
        return Des.decryptString(url, charset, desKey);
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    @Override
    public void manageConverters() {
        long expireMills = streamServerConfig.getExpireMills();
        if (expireMills == -1) {
            log.info("因过期时间设为永久，管理任务没有运行:{}", new Date());
            return;
        }
        log.info("管理任务开始运行:{}", new Date());
        activeStreamMap.forEach((key, lastAccessTime) -> {
            if (lastAccessTime == null) {
                return;
            }
            long accessTime = lastAccessTime.getTime();
            long currentTime = System.currentTimeMillis();
            long diff = currentTime - accessTime;
            if (expireMills >= diff) {
                Converter flvConverter = flvConverters.get(key);
                if (flvConverter != null) {
                    flvConverter.softClose();
                    flvConverters.remove(key);
                    log.info("管理任务移去FLV转流任务:{}", key);
                }
                Converter hlsConverter = hlsConverters.get(key);
                if (hlsConverter != null) {
                    hlsConverter.softClose();
                    hlsConverters.remove(key);
                    log.info("管理任务移去HLS转流任务:{}", key);
                }
                activeStreamMap.remove(key);
            }
        });
    }

    /**
     * 打开默认需要打开的流
     */
    @PostConstruct
    public void openDefaultStreams() throws IOException {
        File defaultOpenListFile = new File("convertToHlsList.default");
        if (defaultOpenListFile.exists()) {
            log.info("已找到默认hls配置文件:{}，准备开启", defaultOpenListFile.getAbsolutePath());
            List<String> hlsUrlList = FileUtils.readLines(defaultOpenListFile, "utf-8");
            StreamServiceImpl that = this;
            hlsUrlList.stream().forEach((url -> {
                Thread thread = new Thread(() -> {
                    log.info("准备开启{}的转流", url);
                    that.open(url, "hls", null, null);
                    log.info("{}的转流开启成功!!", url);
                });
                thread.start();
            }));
        } else {
            log.info("未找到默认hls配置文件:{}", defaultOpenListFile.getAbsolutePath());
        }
    }
}
