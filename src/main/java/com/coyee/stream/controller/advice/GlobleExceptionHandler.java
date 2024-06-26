package com.coyee.stream.controller.advice;

import com.coyee.stream.bean.JsonResult;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hxfein
 * @className: GlobleExceptionHandler
 * @description: 全局异常处理
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobleExceptionHandler {

    /**
     * 404 异常捕捉处理
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = org.springframework.web.servlet.NoHandlerFoundException.class)
    public JsonResult errorHandler(org.springframework.web.servlet.NoHandlerFoundException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.result(404, "服务接口不存在。");
    }

    /**
     * 参数校验出错
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    public JsonResult errorHandler(BindException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "参数不正确," + ex.getMessage());
    }

    /**
     * 参数校验出错
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = BindValidationException.class)
    public JsonResult errorHandler(BindValidationException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "参数不正确," + ex.getMessage());
    }

    /**
     * 请求的方式不支持
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public JsonResult errorHandler(HttpRequestMethodNotSupportedException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "Http 方式不正确," + ex.getMessage());
    }

    /**
     * 非法的参数异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = IllegalArgumentException.class)
    public JsonResult errorHandler(IllegalArgumentException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error(500, "非法的参数，" + ex.getMessage());
    }

    /**
     * 类型异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpMediaTypeException.class)
    public JsonResult errorHandler(HttpMediaTypeException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "服务异常,请检查参数及调用方式。详情内容为： " + ex.getMessage());
    }

    /**
     * 拦截捕捉 CMSException.class
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpMessageConversionException.class)
    public JsonResult errorHandler(HttpMessageConversionException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "参数不正确。详情内容为：" + ex.getMessage());
    }

    /**
     * 全局异常捕捉处理
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public JsonResult errorHandler(Exception ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "服务异常,暂时不可用。" + ex.getMessage());
    }

    /**
     * 参数不能解析
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public JsonResult errorHandler(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage(), ex);
        return JsonResult.error().put("message", "参数解析异常，请检查参数格式。" + ex.getMessage());
    }
}
