# quartz框架

## 参考资料

https://www.quartz-scheduler.org/documentation/quartz-2.3.0/quick-start.html
https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/


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

###  抽象模型

Job
任务框架仅仅抽象所有任务被调用的统一接口.
用户实现的Job类,即不同任务的具体执行逻辑.

JobDetail
向Scheduler注册同一个Job的多个实例(且每个实例所携带的初始属性不同),此时就需要JobDetail这个抽象模型了.
JobDetail即Job instance所携带的实例属性.
这些实例属性中有一部分是metadata信息(即后续不会被更改, 比如JobKey/JobClass等),还有一部分属性是会改动(比如JobDataMap)
Defines the job that needs to be executed, including the Job class and any necessary parameters.

Scheduler
A Scheduler maintains a registry of JobDetails and Triggers.
This is the main interface of a Quartz Scheduler.
The main interface for managing job scheduling. It is responsible for adding, removing, and executing jobs based on the triggers.

Trigger
Specifies when and how often a job should run.


JobStore
The interface to be implemented by classes that want to provide a Job and Trigger storage mechanism for the org.quartz.core.QuartzScheduler's use.
Storage of Jobs and Triggers should be keyed on the combination of their name and group for uniqueness.

```text
Trigger实例的状态,状态枚举值参考org.quartz.impl.jdbcjobstore.Constants.STATE_XXX

STATE_WAITING
STATE_ACQUIRED
STATE_EXECUTING
STATE_COMPLETE
STATE_BLOCKED
STATE_ERROR
STATE_PAUSED
STATE_PAUSED_BLOCKED
STATE_DELETED
STATE_MISFIRED (Deprecated)

STATE_MISFIRED (Deprecated)
Whether a trigger has misfired is no longer a state, 
but rather now identified dynamically by whether the trigger's next fire time is more than the misfire threshold time in the past
是否触发器发生了误触发（misfire）不再是一个静态的状态,而是动态地通过判断触发器的下次触发时间是否比误触发阈值时间更早来确定
```


QuartzScheduler(核心实现类,不面向用户)
StdScheduler(QuartzScheduler的proxy,面向用户)
QuartzSchedulerThread(主线程,负责fire Trigger,即在main loop中查询所有的Trigger,一步步更改Trigger的状态和提交待执行的Job)
WorkerThread(工作线程,执行Job,在worker loop中等待后执行新的Job)





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




## 注意事项
1. 对于jdbc类型的持久化JobStore,JobStore在维护Trigger状态时,使用了数据库行锁来保护状态一致性的.
