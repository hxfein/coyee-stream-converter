package com.coyee.stream.exception;

/**
 * @author hxfein
 * @className: ServiceException
 * @description:业务异常
 * @date 2023/2/9 15:46
 * @version：1.0
 */
public class ServiceException extends RuntimeException {
    public ServiceException(String msg,Throwable root){
        super(msg,root);
    }
    public ServiceException(String msg){
        super(msg);
    }
}
