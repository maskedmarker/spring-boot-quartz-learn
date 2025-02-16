package org.example.learn.spring.boot.quartz.basic.hello;

import org.example.learn.spring.boot.quartz.basic.hello.job.HelloJob;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.util.concurrent.TimeUnit;

import static org.quartz.impl.matchers.GroupMatcher.groupEquals;


/**
 *
 */
public class Ch003Test {

    /**
     * Listing Jobs in the Scheduler
     */
    @Test
    public void test0() throws Exception {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        // and start it off
        scheduler.start();

        JobDetail job1 = JobBuilder.newJob(HelloJob.class).withIdentity("job1", "group1").build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(5).repeatForever())
                .build();
        JobDetail job2 = JobBuilder.newJob(HelloJob.class).withIdentity("job2", "group2").build();
        Trigger trigger2 = TriggerBuilder.newTrigger()
                .withIdentity("trigger2", "group2")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(5).repeatForever())
                .build();


        scheduler.scheduleJob(job1, trigger);
        scheduler.scheduleJob(job2, trigger2);

        // enumerate each job group
        for (String group : scheduler.getJobGroupNames()) {
            // enumerate each job in group
            for (JobKey jobKey : scheduler.getJobKeys(groupEquals(group))) {
                System.out.println("Found job identified by: " + jobKey);
            }
        }

        // 让scheduler执行一会任务,再shutdown scheduler
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        scheduler.shutdown();
    }
}
