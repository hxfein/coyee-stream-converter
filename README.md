# 监控视频流转换

#### 一. 介绍

用于将rtsp等协议的视频流转换为rtmp或hls协议的视频流，使其能在android、ios等设备上播放。

#### 二. 将视频流转为HLS并播放

1. 调用 http://{server}:{port}/converter/register 接口,输入${直播源地址url}和"hls",将直播地址编码处理为key
2. 使用hls.js播放 http://{server}:{port}/stream/live/{key}/play.m3u8

 _客户端调用示例参照:hls.html_ 

#### 三. 将视频流转为FLV并播放

1. 调用 http://{server}:{port}/converter/register 接口,输入${直播源地址url}和"flv",将直播地址编码处理为key
2. 使用flv.js播放 http://{server}:{port}/stream/live/{key}.flv

 _客户端调用示例参照:flv.html_ 

启动后访问:  
http://{server}:{port}/stream/static/hls.html 可查看rtsp转hls示例

http://{server}:{port}/stream/static/flv.html 可查看rtsp转rtmp示例







