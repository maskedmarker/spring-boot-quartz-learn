package org.example.learn.spring.boot.quartz.basic.hello;

import org.example.learn.spring.boot.quartz.basic.hello.job.HelloJob;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.util.concurrent.TimeUnit;


/**
 * StdSchedulerFactory
 * An implementation of SchedulerFactory that does all of its work of creating a QuartzScheduler instance based on the contents of a Properties file.
 * By default a properties file named "quartz.properties" is loaded from the 'current working directory'.
 * If that fails, then the "quartz.properties" file located (as a resource) in the org/quartz package is loaded.
 * If you wish to use a file other than these defaults, you must define the system property 'org.quartz.properties' to point to the file you want.
 */
public class Ch001StdSchedulerFactoryTest {

    @Test
    public void test0() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.start();

            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }

    @Test
    public void test1() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.start();

            // define the jobDetail and tie it to our HelloJob class
            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .build();

            // Trigger the jobDetail to run now, and then repeat every 40 seconds
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(5)
                            .repeatForever())
                    .build();

            // Tell quartz to schedule the jobDetail using our trigger
            scheduler.scheduleJob(jobDetail, trigger);

            // 让scheduler执行一会任务,再shutdown scheduler
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 先添加一个job(没有声明对应的trigger)
     * 后续添加一个trigger,关联已存在的job
     */
    @Test
    public void test2() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .build();
            // Add the the job to the scheduler's store for Later Use
            scheduler.addJob(jobDetail, false);


            // Define a Trigger that will fire "now" and associate it with the existing job
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(5)
                            .repeatForever())
                    .forJob(JobKey.jobKey("job1", "group1"))
                    .build();
            // Schedule the trigger
            scheduler.scheduleJob(trigger);

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
