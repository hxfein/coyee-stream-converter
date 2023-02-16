package com.coyee.stream.controller;

import com.coyee.stream.bean.JsonResult;
import com.coyee.stream.converter.Converter;
import com.coyee.stream.converter.ConverterFactory;
import com.coyee.stream.converter.FlvConverter;
import com.coyee.stream.converter.HlsConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author hxfein
 * @className: StreamController
 * @description: 流服务controller
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
@RestController
public class StreamController {

    /**
     * 注册播放器
     *
     * @param url    监控地址
     * @param format 期望的格式(flv,hls)
     * @return
     */
    @PostMapping(value = "/converter/register")
    @ResponseBody
    public JsonResult register(String url, String format) {
        try {
            Converter converter = ConverterFactory.register(url, format);
            return JsonResult.ok().data(converter.getKey());
        } catch (Exception er) {
            return JsonResult.error(er.getMessage());
        }
    }

    /**
     * 打开一个flv流
     *
     * @param key      加密后的流地址
     * @param response
     * @param request
     */
    @GetMapping(value = "/live/{key}.flv")
    public void flvLive(HttpServletResponse response,
                        HttpServletRequest request, @PathVariable(value = "key") String key) throws InterruptedException {
        Converter converter = ConverterFactory.get(key);
        if (converter == null || converter instanceof FlvConverter == false) {
            JsonResult.sendError(response, 500, "转换器未注册!");
            return;
        }
        FlvConverter flvConverter = (FlvConverter) converter;
        try {
            flvConverter.play(request, response);
        } catch (InterruptedException e) {
            try {
                String url = flvConverter.getEndpoint();
                ConverterFactory.cancle(key);
                flvConverter = ConverterFactory.registerFlv(url, key);
                flvConverter.play(request, response);
            } catch (InterruptedException er) {
                log.error("流转换任务转换失败:{}",er.getMessage());
            }
        }
    }

    /**
     * 开启hls流的转换,返回播放地址
     *
     * @param key      加密后的流地址
     * @param response
     * @param request
     */
    @GetMapping(value = "/live/{key}/play.m3u8")
    public void hlsLive(HttpServletResponse response,
                        HttpServletRequest request, @PathVariable(value = "key") String key) {
        InputStream inputStream = null;
        try {
            ConverterFactory.keepAlive(key);
            Converter converter = ConverterFactory.get(key);
            if (converter == null || converter instanceof HlsConverter == false) {
                JsonResult.sendError(response, 500, "转换器未注册!");
                return;
            }
            String url = converter.getEndpoint();
            ConverterFactory.registerHls(url, key);//检查生成的文件，一旦过期马上重新生成，以此解决流中断导致文件不及时的问题
            HlsConverter hlsConverter = (HlsConverter) converter;
            File indexFile = hlsConverter.getIndexFile();
            if (indexFile.exists() == false) {
                JsonResult.sendError(response, 404, "没有找到文件!");
                return;
            }
            response.setContentType("application/vnd.apple.mpegurl");
            OutputStream output = response.getOutputStream();
            inputStream = new FileInputStream(indexFile);
            IOUtils.copy(inputStream, output);
            IOUtils.closeQuietly(output);
        } catch (Exception er) {
            log.error("获取m3u8文件出错", er);
            JsonResult.sendError(response, 500, er.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 返回ts文件流
     *
     * @param key
     * @param response
     * @param request
     * @param filename
     * @return
     */
    @GetMapping(value = "/live/{key}/{filename}.ts")
    public void playTS(HttpServletResponse response,
                       HttpServletRequest request, @PathVariable(value = "key") String key, @PathVariable(value = "filename") String filename) {
        InputStream inputStream = null;
        try {
            ConverterFactory.keepAlive(key);
            Converter converter = ConverterFactory.get(key);
            if (converter == null || converter instanceof HlsConverter == false) {
                response.sendError(500, "没有找到转换器!");
                return;
            }
            HlsConverter hlsConverter = (HlsConverter) converter;
            File tsFile = hlsConverter.getTsFile(filename);
            if (tsFile.exists() == false) {
                response.sendError(404, "没有找到文件!");
                return;
            }
            response.setContentType("application/x-linguist");
            OutputStream output = response.getOutputStream();
            inputStream = new FileInputStream(tsFile);
            IOUtils.copy(inputStream, output);
        } catch (ClientAbortException er) {
            log.debug("客户端中断:" + er.getMessage());
        } catch (Exception er) {
            log.error("获取ts出错", er);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }


}
