package com.coyee.stream.service;

import com.coyee.stream.converter.HlsConverter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @author hxfein
 * @className: IStreamService
 * @description: 流服务相关接口
 * @date 2022/5/12 14:32
 * @version：1.0
 */
public interface IStreamService {

    /**
     * 打开转换流
     *
     * @param url
     * @param format
     * @param response
     * @param request
     * @return 播放地址
     */
    String open(String url, String format, HttpServletResponse response, HttpServletRequest request);

    /**
     * 更新流的上次访问时间
     *
     * @param key
     */
    boolean activeStream(String key);

    /**
     * 流地址加密
     *
     * @param url
     * @return
     */
    String encode(String url);

    /**
     * 流地址解密
     *
     * @param url
     * @return
     */
    String decode(String url);

    /**
     * 定时任务关闭长期未使用的转换器
     * 每5分钟执行一次
     */
    void manageConverters();

}
