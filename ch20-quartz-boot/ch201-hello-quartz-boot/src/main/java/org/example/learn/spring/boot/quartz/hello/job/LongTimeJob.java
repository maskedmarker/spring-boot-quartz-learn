package org.example.learn.spring.boot.quartz.hello.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LongTimeJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(LongTimeJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("{} is working at {}", LongTimeJob.class.getSimpleName(), new Date());
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(7));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("{} complete at {}", LongTimeJob.class.getSimpleName(), new Date());
    }
}
