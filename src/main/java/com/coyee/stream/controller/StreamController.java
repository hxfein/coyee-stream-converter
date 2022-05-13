package com.coyee.stream.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coyee.stream.config.StreamServerConfig;
import com.coyee.stream.result.JsonResult;
import com.coyee.stream.service.IStreamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;


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

    @Resource
    private StreamServerConfig streamServerConfig;

    @Autowired
    private IStreamService service;

    /**
     * 开启hls流的转换,返回播放地址
     *
     * @param key  加密后的流地址
     * @param response
     * @param request
     */
    @GetMapping(value = "/live/openHls/{key}")
    @ResponseBody
    public JsonResult openHls(@PathVariable(value = "key") String key, HttpServletResponse response,
                              HttpServletRequest request) {
        String realUrl = service.decode(key);
        String playUrl = service.open(realUrl, "hls", response, request);
        return JsonResult.ok().data(playUrl);
    }

    /**
     * 保持转换器为运行状态
     *
     * @param key
     * @return
     */
    @GetMapping(value = "/live/ping/{key}")
    @ResponseBody
    public String ping(@PathVariable(value = "key") String key) {
        boolean success = service.activeStream(key);
        if (success == true) {
            return "ok";
        } else {
            return "not found";
        }
    }

    /**
     * 加密地址
     *
     * @param url
     * @return
     */
    @PostMapping(value = "/url/encode")
    @ResponseBody
    public JsonResult encode(String url) {
        String key = service.encode(url);
        return JsonResult.ok().data(key);
    }

    /**
     * 加密地址
     *
     * @param urlHex
     * @return
     */
    @PostMapping(value = "/url/decode")
    @ResponseBody
    public JsonResult decode(String urlHex) {
        String url = service.decode(urlHex);
        return JsonResult.ok().data(url);
    }


    /**
     * 返回m3u8文件流
     *
     * @param key
     * @param response
     * @param request
     * @return
     */
    @GetMapping(value = "/live/{url}/play.m3u8")
    public String playM3U8(@PathVariable(value = "url") String key, HttpServletResponse response,
                           HttpServletRequest request) throws IOException {
        InputStream inputStream = null;
        try {
            String hlsStoreDir = streamServerConfig.getHlsStoreDir();
            String m3u8Path = FilenameUtils.separatorsToSystem(hlsStoreDir + File.separator + key + File.separator + "play.m3u8");
            File m3u8File = new File(m3u8Path);
            if (m3u8File.exists() == false) {
                response.sendError(404);
                return null;
            }
            response.setContentType("audio/x-mpegur");
            OutputStream output = response.getOutputStream();
            inputStream = new FileInputStream(m3u8File);
            IOUtils.copy(inputStream, output);
            return null;
        } catch (Exception er) {
            log.error("获取m3u8出错", er);
            return null;
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * 返回ts文件流
     *
     * @param key
     * @param response
     * @param request
     * @return
     */
    @GetMapping(value = "/live/{url}/{index}.ts")
    public String playTS(@PathVariable(value = "url") String key, @PathVariable(value = "index") String index, HttpServletResponse response,
                         HttpServletRequest request) throws IOException {
        InputStream inputStream = null;
        try {
            String hlsStoreDir = streamServerConfig.getHlsStoreDir();
            String tsPath = FilenameUtils.separatorsToSystem(hlsStoreDir + File.separator + key + File.separator + index + ".ts");
            File tsFile = new File(tsPath);
            if (tsFile.exists() == false) {
                response.sendError(404);
                return null;
            }
            response.setContentType("application/x-linguist");
            OutputStream output = response.getOutputStream();
            inputStream = new FileInputStream(tsFile);
            IOUtils.copy(inputStream, output);
            return null;
        } catch (Exception er) {
            log.error("获取ts出错", er);
            return null;
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * 打开一个flv流
     *
     * @param key 加密后的流地址
     * @param response
     * @param request
     */
    @GetMapping(value = "/live/{key}.flv")
    public void flvLive(@PathVariable(value = "key") String key, HttpServletResponse response,
                        HttpServletRequest request) {
        String realUrl = service.decode(key);
        service.open(realUrl, "flv", response, request);
    }

}
