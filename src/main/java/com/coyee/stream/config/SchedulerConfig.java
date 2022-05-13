package com.coyee.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author hxfein
 * @className: SchedulerConfig
 * @description: 定时任务配置
 * @date 2022/5/12 14:32
 * @version：1.0
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		// 线程池大小
		scheduler.setPoolSize(3);
		// 线程名字前缀
		scheduler.setThreadNamePrefix("task-thread-");
		return scheduler;
	}

}
