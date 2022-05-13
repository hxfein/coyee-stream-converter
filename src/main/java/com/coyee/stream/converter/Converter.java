package com.coyee.stream.converter;

import java.io.IOException;

import javax.servlet.AsyncContext;

/**
 * @author hxfein
 * @className: Converter
 * @description: 转换器接口
 * @date 2022/5/12 14:32
 * @version：1.0
 */
public interface Converter {

	/**
	 * 添加一个流输出
	 *
	 * @param entity
	 */
	void addOutputStreamEntity(String key, AsyncContext entity) throws IOException;

	/**
	 * 要求关闭转换器
	 */
	void softClose();


}
