package com.coyee.stream.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

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
    private int hlsTime=5;
    /**
     * 最大分片数
     */
    private int hlsWrap=10;
    /**
     * 播放列表数
     */
    private int hlsListSize=0;

    /**
     * 系统启动时，先把hlsStoreDir里面的文件清除掉
     */
    @PostConstruct
    public void onStreamServerStart() throws IOException {
        File dir=new File(hlsStoreDir);
        log.info("准备清除hls目录的残留文件");
        File[] children=dir.listFiles();
        for(File child:children){
            if(child.isDirectory()) {
                FileUtils.deleteDirectory(child);
                log.info("目录 已被清除:{}",child.getAbsolutePath());
            }else{
                FileUtils.deleteQuietly(child);
                log.info("文件 已被清除:{}",child.getAbsolutePath());
            }
        }
    }
}
