package com.coyee.stream.converter;

import com.coyee.stream.config.StreamServerConfig;
import lombok.Data;
import net.jodah.expiringmap.ExpiringMap;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;

/**
 * @author hxfein
 * @className: Converter
 * @description: 转换器接口
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Data
public abstract class Converter extends Thread {
    /**
     * 实际地址
     */
    protected String endpoint;
    /**
     * 上次访问时间
     */
    protected Date lastAccessTime;
    /**
     * 是否正在运行
     */
    protected boolean running = true;
    /**
     * 关键字
     */
    protected String key;
    /**
     * 流服务配置
     */
    protected StreamServerConfig streamServerConfig;
    /**
     * 回调
     */
    protected Callback callback;

    /**
     * 要求关闭转换器
     */
    public abstract void softClose();

    /**
     * 设置过期时间
     *
     * @param converterMap
     */
    public void setExpireTime(ExpiringMap<String, Converter> converterMap) {
        long expireMills = streamServerConfig.getExpireMills();
        if (expireMills == -1) {
            converterMap.put(key, this, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } else {
            converterMap.put(key, this, expireMills, TimeUnit.MILLISECONDS);
        }
        this.setLastAccessTime(new Date());
    }

    /**
     * 回调
     */
    public interface Callback {
        void notify(Event event, Object data);
    }

    /**
     * 回调事件类型
     */
    public enum Event {
        Active,
        Stop
    }
}
