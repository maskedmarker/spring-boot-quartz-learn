package org.example.learn.spring.scheduling.hello.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TimeReportTask {

    private static Logger logger = LoggerFactory.getLogger(TimeReportTask.class);

    @Scheduled(cron = "0/3 * * * * *")
    public void task1() {
        logger.info("{}: it is at {}", "task1", new Date());
    }

    @Scheduled(cron = "0/7 * * * * *")
    public void task2() {
        logger.info("{}: it is at {}", "task2", new Date());
    }
}
