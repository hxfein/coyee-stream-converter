package com.coyee.stream;

import com.coyee.stream.config.StreamServerConfig;
import com.coyee.stream.converter.ConverterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * @author hxfein
 * @className: StreamApplication
 * @description: 流服务启动类
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
@EnableCaching // 开启缓存
@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = "com.coyee.stream")
public class StreamApplication implements ApplicationRunner {
    @Resource
    private StreamServerConfig streamServerConfig;

    public static void main(String[] args) {
        SpringApplication.run(StreamApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        ConverterFactory.init(streamServerConfig);
        registerFromFile(args, "hlsSourceFile", "hls");
        registerFromFile(args, "flvSourceFile", "flv");
    }

    /**
     * 从文件中初始化转换器
     *
     * @param args
     * @param variableName
     * @param format
     */
    private void registerFromFile(ApplicationArguments args, String variableName, String format) {
        List<String> sourceFileList = args.getOptionValues(variableName);
        if (sourceFileList == null || sourceFileList.isEmpty()) {
            return;
        }
        String filename = sourceFileList.get(0);
        File sourceFile = new File(filename);
        if (sourceFile.exists()) {
            ConverterFactory.registerFromFile(sourceFile, format);
        } else {
            log.error("批量初始化的文件{}不存在!", sourceFileList);
        }
    }
}
