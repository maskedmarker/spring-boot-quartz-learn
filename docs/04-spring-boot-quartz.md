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



By default:
Quartz jobs are executed in a single-threaded manner within the same scheduler, 
so if the previous job execution has not finished by the time the next trigger occurs, the next execution will be missed or queued depending on the configuration.


### auto-configure

QuartzAutoConfiguration先解析spring.quartz配置,
然后向spring容器中添加SchedulerFactoryBean
如果发现job-store-type配置是jdbc时,还会为SchedulerFactoryBean配置DataSource/TransactionManager,同时向容器添加QuartzDataSourceInitializer,保证quartz脚本被初始化

主动声明,Scheduler(SchedulerFactoryBean)依赖QuartzDataSourceInitializer
```text
@Configuration
static class QuartzSchedulerDependencyConfiguration {

    @Bean
    static SchedulerDependsOnBeanFactoryPostProcessor quartzSchedulerDataSourceInitializerDependsOnBeanFactoryPostProcessor() {
        return new SchedulerDependsOnBeanFactoryPostProcessor(QuartzDataSourceInitializer.class);
    }
}
```

```text
org.springframework.scheduling.quartz.SchedulerFactoryBean.initSchedulerFactory

```