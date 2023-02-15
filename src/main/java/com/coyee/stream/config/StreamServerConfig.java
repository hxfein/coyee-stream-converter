package com.coyee.stream.config;

import com.coyee.stream.converter.ConverterFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author hxfein
 * @className: StreamServerConfig
 * @description: 流服务配置
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Component
@Data
@ConfigurationProperties("streamserver")
@Slf4j
public class StreamServerConfig {
    /**
     * 加密key
     */
    private String desKey;
    /**
     * 转为hls协议时m3u8、TS文件的存储目录
     */
    private String hlsStoreDir;
    /**
     * 单个分片播放时间
     */
    private int hlsTime = 5;
    /**
     * 最大分片数
     */
    private int hlsWrap = 20;
    /**
     * 播放列表数
     */
    private int hlsListSize = 10;
    /**
     * 没有观众时流的关闭时间,-1为不关闭
     */
    private long expireMills = 1000 * 60 * 5;
    /**
     * 当m3u8文件超过时间没有更新时，则认为已过期
     */
    private long m3u8ExpireMills=1000*60*3;

}
