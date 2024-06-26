package com.coyee.stream.bean;

import java.io.Serializable;
import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * @author hxfein
 * @className: JsonResult
 * @description: 接口返回结果结果封装
 * @date 2022/5/12 14:32
 * @version：1.0
 */
public class JsonResult extends HashMap<String, Object> implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final int SUCCESS = 200;

    public JsonResult() {
    }

    /**
     * 返回成功
     */
    public static JsonResult ok() {
        return ok("操作成功");
    }

    /**
     * 返回成功
     */
    public static JsonResult okFallBack() {
        return okFallBack("操作成功");
    }

    /**
     * 返回成功
     */
    public JsonResult put(Object obj) {
        return this.put("data", obj);
    }

    /**
     * 返回成功
     */
    public static JsonResult ok(String message) {
        return result(200, message);
    }

    /**
     * 降级函数 - 返回成功
     */
    public static JsonResult okFallBack(String message) {
        return result(205, message);
    }

    /**
     * 返回成功
     */
    public static JsonResult result(int code, String message) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.put("timestamp", System.currentTimeMillis());
        jsonResult.put("status", code);
        jsonResult.put("message", message);
        return jsonResult;
    }

    /**
     * 设置数据
     *
     * @param data
     * @return
     */
    public JsonResult data(Object data) {
        this.put("data", data);
        return this;
    }

    /**
     * 返回失败
     */
    public static JsonResult error() {
        return error("操作失败");
    }

    /**
     * 返回失败
     */
    public static JsonResult error(String message) {
        return error(500, message);
    }

    /**
     * 返回失败
     */
    public static JsonResult error(int code, String message) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.put("timestamp", System.currentTimeMillis());
        jsonResult.put("status", code);
        jsonResult.put("message", message);
        return jsonResult;
    }

    /**
     * 设置code
     */
    public JsonResult setCode(int code) {
        super.put("status", code);
        return this;
    }

    /**
     * 设置message
     */
    public JsonResult setMessage(String message) {
        super.put("message", message);
        return this;
    }

    /**
     * 放入object
     */
    @Override
    public JsonResult put(String key, Object object) {
        super.put(key, object);
        return this;
    }

    /**
     * 权限禁止
     */
    public static JsonResult forbidden(String message) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.put("timestamp", System.currentTimeMillis());
        jsonResult.put("status", 401);
        jsonResult.put("message", message);
        return jsonResult;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public JSONObject toJSONObject() {
        return JSONObject.parseObject(toString());
    }

    /**
     * 发送错误
     * @param response
     * @param code
     * @param msg
     */
    public static void sendError(HttpServletResponse response, int code, String msg){
        try {
            response.sendError(code, msg);
        }catch(Exception er){
            //ignore
        }
    }

}