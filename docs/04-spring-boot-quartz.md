# spring-boot与quartz

spring-boot的auto-configure模块支持quartz框架


## 核心类

### QuartzAutoConfiguration

org.springframework.scheduling.quartz.SchedulerFactoryBean.afterPropertiesSet
```text
public void afterPropertiesSet() throws Exception {
    if (this.dataSource == null && this.nonTransactionalDataSource != null) {
        this.dataSource = this.nonTransactionalDataSource;
    }

    // Initialize the Scheduler instance...
    this.scheduler = prepareScheduler(prepareSchedulerFactory());
    try {
        registerListeners();
        registerJobsAndTriggers();
    }
    catch (Exception ex) {
        try {
            this.scheduler.shutdown(true);
        }
        catch (Exception ex2) {
            logger.debug("Scheduler shutdown exception after registration failure", ex2);
        }
        throw ex;
    }
}
```

### QuartzSchedulerThread

org.quartz.core.QuartzSchedulerThread.run
The main processing loop of the QuartzSchedulerThread.

```text
if (qsRsrcs.getThreadPool().runInThread(shell) == false) 
```