package org.example.learn.spring.boot.quartz.basic.hello;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 *
 */
public class Ch004MisfireTest {

    private static final Logger logger = LoggerFactory.getLogger(Ch004MisfireTest.class);

    private static final int WORKING_INTERVAL = 2;

    public static class Ch004Job implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("{} is working at {}", Ch004Job.class.getSimpleName(), new Date());
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(WORKING_INTERVAL * 3));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            logger.info("{} complete at {}", Ch004Job.class.getSimpleName(), new Date());
        }
    }


    /**
     * misfire策略
     *
     * MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     *
     * Instructs the Scheduler that the Trigger will never be evaluated for a misfire situation,
     * and that the scheduler will simply try to fire it as soon as it can,
     * and then update the Trigger as if it had fired at the proper time.
     * NOTE: if a trigger uses this instruction, and it has missed several of its scheduled firings,
     * then several rapid firings may occur as the trigger attempt to catch back up to where it would have been.
     *
     * For example, a SimpleTrigger that fires every 15 seconds which has misfired for 5 minutes will fire 20 times once it gets the chance to fire.
     *
     * 通过不更新nextFireTime来实现短时间内多次执行,从而追赶到最新时间,使之不再misfired
     */
    @Test
    public void test0() throws Exception {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDetail job1 = JobBuilder.newJob(Ch004Job.class).withIdentity("job1", "group1").build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(WORKING_INTERVAL)
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires()) //
                .build();


        scheduler.scheduleJob(job1, trigger);


        // 让scheduler执行一会任务,再shutdown scheduler
        Thread.sleep(TimeUnit.SECONDS.toMillis(100000));
        scheduler.shutdown();
    }

    /**
     * misfire策略
     * MISFIRE_INSTRUCTION_FIRE_NOW
     * upon a mis-fire situation, the SimpleTrigger wants to be fired now by Schedule
     */
    @Test
    public void test2() throws Exception {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDetail job1 = JobBuilder.newJob(Ch004Job.class).withIdentity("job1", "group1").build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(WORKING_INTERVAL)
                        .repeatForever()
                        .withMisfireHandlingInstructionFireNow()) // 发现misfire了,立即执行
                .build();


        scheduler.scheduleJob(job1, trigger);


        // 让scheduler执行一会任务,再shutdown scheduler
        Thread.sleep(TimeUnit.SECONDS.toMillis(100000));
        scheduler.shutdown();
    }
}
