package com.coyee.stream.converter;

import com.coyee.stream.config.StreamServerConfig;
import com.coyee.stream.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author hxfein
 * @className: ConverterFactory
 * @description:转换器工厂
 * @date 2023/2/9 15:16
 * @version：1.0
 */
@Slf4j
public class ConverterFactory {
    private static ExpiringMap<String, Converter> converterMap = ExpiringMap.builder().variableExpiration().build();
    private static StreamServerConfig streamServerConfig;

    /**
     * 初始化
     *
     * @param config
     */
    public static void init(StreamServerConfig config) {
        streamServerConfig = config;
        converterMap.addExpirationListener(new ConverterExpirationListener());
    }

    /**
     * 注册地址，返回播放器的KEY
     *
     * @param url
     * @return
     */
    public static Converter register(String url, String format) {
        Converter converter = null;
        String key = DigestUtils.md5Hex(url + "@" + format);
        if (StringUtils.equals(format, "flv")) {
            converter = registerFlv(url, key);
        } else if (StringUtils.equals(format, "hls")) {
            converter = registerHls(url, key);
        } else {
            throw new ServiceException("未知的转换器类型:" + format + "!");
        }
        converter.setName("Converter." + format + "." + key);
        return converter;
    }

    /**
     * 从文件中批量注册
     * @param file
     * @param format
     */
    public static void registerFromFile(File file,String format){
        try {
            log.info("从文件{}中初始化{}流的转换！",file,format);
            List<String> lines = FileUtils.readLines(file, "utf-8");
            lines.forEach(line -> {
                register(line, format);
            });
        }catch(IOException er){
            throw new ServiceException("注册失败",er);
        }
    }

    /**
     * 取消注册
     *
     * @param key
     */
    public static void cancle(String key) {
        Converter converter = converterMap.get(key);
        if (converter != null) {
            converter.softClose();
        }
        converterMap.remove(key);
    }

    public static FlvConverter registerFlv(String url, String key) {
        FlvConverter converter = (FlvConverter) converterMap.get(key);
        if (converter != null) {
            return converter;
        }
        final FlvConverter that = converter = new FlvConverter();
        converter.setCallback((type, data) -> {
            if (type == Converter.Event.Active) {
                that.setExpireTime(converterMap);
            }
        });
        converter.setStreamServerConfig(streamServerConfig);
        converter.setEndpoint(url);
        converter.setKey(key);
        converter.setExpireTime(converterMap);
        return converter;
    }

    public static HlsConverter registerHls(String url, String key) {
        HlsConverter converter = (HlsConverter) converterMap.get(key);
        if (converter == null) {
            converter = new HlsConverter();
            converter.setStreamServerConfig(streamServerConfig);
            converter.setEndpoint(url);
            converter.setKey(key);
        }
        Thread.State state = converter.getState();
        if (state == Thread.State.NEW) {
            converter.startAndWait();
        } else if (state == Thread.State.TERMINATED) {
            cancle(key);
            return registerHls(url, key);
        } else {
            File indexFile = converter.getIndexFile();
            if (indexFile.exists() == false){
                cancle(key);
                return registerHls(url, key);
            }
        }
        converter.setExpireTime(converterMap);
        return converter;
    }


    /**
     * 转换器保活
     *
     * @param key
     */
    public static void keepAlive(String key) {
        Converter converter = converterMap.get(key);
        if (converter == null) {
            return;
        }
        converter.setExpireTime(converterMap);
    }

    /**
     * 获取转换器
     *
     * @param key
     * @return
     */
    public static Converter get(String key) {
        Converter converter = converterMap.get(key);
        return converter;
    }

    static class ConverterExpirationListener implements ExpirationListener<String, Converter> {
        @Override
        public void expired(String key, Converter converter) {
            converter.softClose();
            log.info("因转换器[{}]({})长时间没有使用，已关闭。", key, converter.getEndpoint());
        }
    }

    public static void main(String[] args) throws Exception{
        ExpiringMap<String, Converter> converterMap = ExpiringMap.builder().variableExpiration().build();
        converterMap.addExpirationListener(new ExpirationListener<String, Converter>(){
            @Override
            public void expired(String s, Converter converter) {
                System.out.println(s+"    "+converter);
            }
        });
        converterMap.put("aaaa",new FlvConverter());
        converterMap.put("bbbb",new HlsConverter(),3000,TimeUnit.MILLISECONDS);
        Thread.sleep(100000000);
    }

}
