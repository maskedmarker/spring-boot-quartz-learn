package org.example.learn.spring.scheduling.hello;

import org.example.learn.spring.scheduling.hello.config.SpringConfig;
import org.example.learn.spring.scheduling.hello.task.TimeReportTask;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Ch001TimeReportTaskTest {

    /**
     * 调用refresh后,spring容器才会将注册bean的实例化,并归容器管理
     */
    @Test
    public void test0() throws Exception {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 注册
        applicationContext.register(TimeReportTask.class);
        applicationContext.register(SpringConfig.class);

        // 调用refresh,触发spring-scheduling的功能生效
        applicationContext.refresh();

        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }

}
