# quartz框架

## 参考资料

https://www.quartz-scheduler.org/documentation/quartz-2.3.0/quick-start.html


https://www.quartz-scheduler.net/documentation/quartz-3.x/tutorial/



## 关键概念

### Scheduler

```text
A Scheduler maintains a registry of JobDetails and Triggers. 
Once registered, the Scheduler is responsible for executing Job s when their associated Trigger s fire (when their scheduled time arrives).

Scheduler instances are produced by a SchedulerFactory.
A scheduler that has already been created/initialized can be found and used through the same factory that produced it. 
After a Scheduler has been created, it is in "stand-by" mode, and must have its start() method called before it will fire any Jobs.


Jobs are to be created by the 'client program', by defining a class that implements the Job interface.
JobDetail objects are then created (also by the client) to define a individual instances of the Job.
JobDetail instances can then be registered with the Scheduler via the scheduleJob(JobDetail, Trigger) or addJob(JobDetail, boolean) method.


Trigger s can then be defined to fire individual Job instances based on given schedules. 
SimpleTriggers are most useful for one-time firings, or firing at an exact moment in time, with N repeats with a given delay between them. 
CronTrigger s allow scheduling based on time of day, day of week, day of month, and month of year.


Job s and Trigger s have a name and group associated with them, which should uniquely identify them within a single Scheduler. 
The 'group' feature may be useful for creating logical groupings or categorizations of Jobs s and Triggerss. 
If you don't have need for assigning a group to a given Jobs of Triggers, then you can use the DEFAULT_GROUP constant defined on this interface.


Stored Jobs can also be 'manually' triggered through the use of the triggerJob(String jobName, String jobGroup) function.


Client programs may also be interested in the 'listener' interfaces that are available from Quartz. 
The JobListener interface provides notifications of Job executions. 
The TriggerListener interface provides notifications of Trigger firings. 
The SchedulerListener interface provides notifications of Scheduler events and errors. 
Listeners can be associated with local schedulers through the ListenerManager interface.
```



## 配置文件

```text
Quartz uses a properties file called quartz.properties. 
This isn’t necessary at first, but to use anything but the most basic configuration it must be located on your classpath.

Quartz is a very configurable application. The best way to configure Quartz is to edit a quartz.properties file, and place it in your application’s classpath.
```

具体配置项的含义参见文档: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/

举例:
```text
org.quartz.scheduler.instanceName = MyScheduler
org.quartz.threadPool.threadCount = 3
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore


The scheduler created by this configuration has the following characteristics:

org.quartz.scheduler.instanceName - This scheduler’s name will be “MyScheduler”.
org.quartz.threadPool.threadCount - There are 3 threads in the thread pool, which means that a maximum of 3 jobs can be run simultaneously.
org.quartz.jobStore.class - All of Quartz’s data, such as details of jobs and triggers, is held in memory (rather than in a database).
```






Job(任务的具体执行内容)
JobDetail(JobDetails are metadata related to a Job implementation, they hold a reference to the Job you want to run and allow you to provide some additional data to your Job)
Scheduler
Trigger
Schedule
Calendar
QuartzScheduler(核心实现类,不面向用户)
StdScheduler(QuartzScheduler的proxy,面向用户)
QuartzSchedulerThread(主线程,负责fire Trigger,即在main loop中查询所有的Trigger,一步步更改Trigger的状态和提交待执行的Job)
WorkerThread(工作线程,执行Job,在worker loop中等待后执行新的Job)
JobStore(JobStore会维护Trigger实例的状态,状态枚举值参考org.quartz.impl.jdbcjobstore.Constants.STATE_XXX)



## 注意事项
1. 基于separation of concerns原则,用户期望被执行的方法被单独放在类Job,其他关于该Job的信息被放在了
2. 对于jdbc类型的持久化JobStore,JobStore在维护Trigger状态时,使用了数据库行锁来保护状态一致性的.
