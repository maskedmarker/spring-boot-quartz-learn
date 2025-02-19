package org.example.learn.spring.scheduling.hello.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 需要主动开启spring的定时任务功能
 */

@EnableScheduling
@Configuration
public class SchedulingConfig {
}
