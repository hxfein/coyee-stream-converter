# 监控视频流转换

#### 一. 介绍
用于将rtsp等协议的视频流转换为rtmp或hls协议的视频流，使其能在android、ios等设备上播放。  

#### 二. 将视频流转为HLS并播放

1.  调用 http://{server}:{port}/stream/url/encode 接口将直播地址编码处理为key
2.  调用 http://{server}:{port}/stream/live/openHls/{key} 接口开始流转换
3.  使用hls.js播放 http://{server}:{port}/stream/live/{key}/play.m3u8
4.  定时访问 http://{server}:{port}/stream/live/ping/{key} 保持转换任务运行

启动后访问:  http://{server}:{port}/stream/static/hls.html  可查看示例

#### 三. 将视频流转为FLV并播放

1.  调用 http://{server}:{port}/stream/url/encode 接口将直播地址编码处理为key
2.  使用flv.js播放 http://{server}:{port}/stream/live/{key}.flv
3.  定时访问 http://{server}:{port}/stream/live/ping/{key} 保持转换任务运行

启动后访问:  
http://{server}:{port}/stream/static/rtmp.html  可查看rtsp转rtmp示例


http://{server}:{port}/stream/static/hls.html   可查看rtsp转hls示例





