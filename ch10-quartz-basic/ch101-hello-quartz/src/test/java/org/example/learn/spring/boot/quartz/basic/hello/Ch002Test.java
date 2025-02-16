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
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


/**
 *
 */
public class Ch002Test {

    private static final Logger logger = LoggerFactory.getLogger(Ch002Test.class);

    /**
     * 先添加一个job(没有声明对应的trigger)
     * 后续添加一个trigger,关联已存在的job
     */
    @Test
    public void test0() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .storeDurably()
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

    /**
     * Update an existing job
     */
    @Test
    public void test1() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .storeDurably()
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


            // Add the new job to the scheduler, instructing it to "replace"
            //  the existing job with the given name and group (if any)
            JobDetail job1 = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .build();

            // store, and set overwrite flag to 'true'
            scheduler.addJob(job1, true);

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replacing a trigger
     */
    @Test
    public void test2() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .storeDurably()
                    .build();
            // Add the the job to the scheduler's store for Later Use
            scheduler.addJob(jobDetail, false);


            // Define a Trigger that will fire "now" and associate it with the existing job
            Trigger trigger1 = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(5)
                            .repeatForever())
                    .forJob(JobKey.jobKey("job1", "group1"))
                    .build();
            // Schedule the trigger
            scheduler.scheduleJob(trigger1);

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));


            // Define a new Trigger
            Trigger trigger2 = TriggerBuilder.newTrigger()
                    .withIdentity("newTrigger", "group1")
                    .startNow()
                    .build();
            // tell the scheduler to remove the old trigger with the given key, and put the new one in its place
            scheduler.rescheduleJob(TriggerKey.triggerKey("trigger1", "group1"), trigger2);

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updating an existing trigger
     */
    @Test
    public void test3() {
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
                    .storeDurably()
                    .build();
            // Add the the job to the scheduler's store for Later Use
            scheduler.addJob(jobDetail, false);


            // Define a Trigger that will fire "now" and associate it with the existing job
            Trigger trigger1 = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(5)
                            .repeatForever())
                    .forJob(JobKey.jobKey("job1", "group1"))
                    .build();
            // Schedule the trigger
            scheduler.scheduleJob(trigger1);

            Thread.sleep(TimeUnit.SECONDS.toMillis(10));


            // retrieve the trigger
            Trigger oldTrigger = scheduler.getTrigger(TriggerKey.triggerKey("trigger1", "group1"));
            // obtain a builder that would produce the trigger
            TriggerBuilder tb = oldTrigger.getTriggerBuilder();
            // update the schedule associated with the builder, and build the new trigger
            // (other builder methods could be called, to change the trigger in any desired way)
            Trigger newTrigger = tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(10)
                    .withRepeatCount(10))
                    .build();

            scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);

            Thread.sleep(TimeUnit.SECONDS.toMillis(30));
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
