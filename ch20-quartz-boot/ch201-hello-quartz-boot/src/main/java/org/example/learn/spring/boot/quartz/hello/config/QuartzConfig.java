package org.example.learn.spring.boot.quartz.hello.config;

import org.example.learn.spring.boot.quartz.hello.job.HelloJob;
import org.example.learn.spring.boot.quartz.hello.job.LongTimeJob;
import org.example.learn.spring.boot.quartz.hello.job.ShortTimeJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail job1() {
        return JobBuilder.newJob(HelloJob.class)
                .withIdentity("job1", "group1")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger(JobDetail job1) {
        return TriggerBuilder.newTrigger()
                .forJob(job1)
                .withIdentity("trigger1", "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?"))  // Runs every 5 seconds
                .build();
    }

    @Bean
    public JobDetail job2() {
        return JobBuilder.newJob(LongTimeJob.class)
                .withIdentity("job2", "group1")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger2(JobDetail job2) {
        return TriggerBuilder.newTrigger()
                .forJob(job2)
                .withIdentity("trigger2", "group2")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/9 * * * * ?").withMisfireHandlingInstructionFireAndProceed())  // Runs every 5 seconds
                .build();
    }

    @Bean
    public JobDetail job3() {
        return JobBuilder.newJob(ShortTimeJob.class)
                .withIdentity("job3", "group3")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger3(JobDetail job3) {
        return TriggerBuilder.newTrigger()
                .forJob(job3)
                .withIdentity("trigger3", "group3")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/7 * * * * ?").withMisfireHandlingInstructionIgnoreMisfires())  // Runs every 5 seconds
                .build();
    }
}