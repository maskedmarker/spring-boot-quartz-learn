# spring-scheduling

spring-scheduling与quartz是互相独立的框架.


## 核心类

### @EnableScheduling

spring-scheduling功能的启动注解.
@EnableScheduling引入了SchedulingConfiguration,而SchedulingConfiguration向spring容器注册了ScheduledAnnotationBeanPostProcessor

```text
@Import(SchedulingConfiguration.class)
public @interface EnableScheduling {
}


@Configuration
public class SchedulingConfiguration {

	@Bean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public ScheduledAnnotationBeanPostProcessor scheduledAnnotationProcessor() {
		return new ScheduledAnnotationBeanPostProcessor();
	}
}
```

### ScheduledAnnotationBeanPostProcessor

ScheduledAnnotationBeanPostProcessor是spring-scheduling的核心类

```text
public class ScheduledAnnotationBeanPostProcessor
		implements ScheduledTaskHolder, MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor,
		Ordered, EmbeddedValueResolverAware, BeanNameAware, BeanFactoryAware, ApplicationContextAware,
		SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent>, DisposableBean {}		
```

ScheduledAnnotationBeanPostProcessor实现了DestructionAwareBeanPostProcessor(BeanPostProcessor的子接口类)
每个bean初始化后,postProcessAfterInitialization方法从bean中提取关于定时任务的信息(此时还未让TaskScheduler按时调用这些bean的方法)
```text
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof AopInfrastructureBean || bean instanceof TaskScheduler || bean instanceof ScheduledExecutorService) {
        // Ignore AOP infrastructure such as scoped proxies.
        return bean;
    }

    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    // 如果bean有携带@Scheduled注解(at type, method or field level)
    if (!this.nonAnnotatedClasses.contains(targetClass) && AnnotationUtils.isCandidateClass(targetClass, Arrays.asList(Scheduled.class, Schedules.class))) {
        // 只关注方法层面上的@Scheduled注解
        Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<Set<Scheduled>>) method -> {
                    Set<Scheduled> scheduledMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                            method, Scheduled.class, Schedules.class);
                    return (!scheduledMethods.isEmpty() ? scheduledMethods : null);
                });
        if (annotatedMethods.isEmpty()) {
            this.nonAnnotatedClasses.add(targetClass);
            if (logger.isTraceEnabled()) {
                logger.trace("No @Scheduled annotations found on bean class: " + targetClass);
            }
        }
        else {
            annotatedMethods.forEach((method, scheduledMethods) -> scheduledMethods.forEach(scheduled -> processScheduled(scheduled, method, bean)));
            if (logger.isTraceEnabled()) {
                logger.trace(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName + "': " + annotatedMethods);
            }
        }
    }
    return bean;
}

// 试图让TaskScheduler调度这些task(如果registrar还未完成初始化的话,registrar.scheduleXXXTask方法仅仅先暂存起来这些任务,等自己初始化后在调度这些任务)
protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
    Runnable runnable = createRunnable(bean, method);
    if (StringUtils.hasText(cron)) {
        this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(cron, timeZone)))
    }
    long fixedDelay = scheduled.fixedDelay();
    if (fixedDelay >= 0) {
        this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay))
    }
    
    String fixedDelayString = scheduled.fixedDelayString();
    if (StringUtils.hasText(fixedDelayString)) {
        this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay))
    }
}
```

ScheduledAnnotationBeanPostProcessor实现了ApplicationListener<ContextRefreshedEvent>
当容器通知ContextRefreshedEvent后,试图从容器中获取客制化的配置,然后才是真正初始化ScheduledTaskRegistrar(进而真正开始调度任务)

```text
public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext() == this.applicationContext) {
        // Running in an ApplicationContext -> register tasks this late...
        // giving other ContextRefreshedEvent listeners a chance to perform
        // their work at the same time (e.g. Spring Batch's job registration).
        finishRegistration();
    }
}


private void finishRegistration() {
    if (this.scheduler != null) {
        this.registrar.setScheduler(this.scheduler);
    }

    if (this.beanFactory instanceof ListableBeanFactory) {
        Map<String, SchedulingConfigurer> beans = ((ListableBeanFactory) this.beanFactory).getBeansOfType(SchedulingConfigurer.class);
        List<SchedulingConfigurer> configurers = new ArrayList<>(beans.values());
        AnnotationAwareOrderComparator.sort(configurers);
        for (SchedulingConfigurer configurer : configurers) {
            configurer.configureTasks(this.registrar);
        }
    }

    if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
        Assert.state(this.beanFactory != null, "BeanFactory must be set to find scheduler by type");
        try {
            // Search for TaskScheduler bean...
            this.registrar.setTaskScheduler(resolveSchedulerBean(this.beanFactory, TaskScheduler.class, false));
        }
        catch (NoUniqueBeanDefinitionException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Could not find unique TaskScheduler bean - attempting to resolve by name: " +
                        ex.getMessage());
            }
            try {
                this.registrar.setTaskScheduler(resolveSchedulerBean(this.beanFactory, TaskScheduler.class, true));
            }
        }
        catch (NoSuchBeanDefinitionException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Could not find default TaskScheduler bean - attempting to find ScheduledExecutorService: " +
                        ex.getMessage());
            }
            // Search for ScheduledExecutorService bean next...
            try {
                this.registrar.setScheduler(resolveSchedulerBean(this.beanFactory, ScheduledExecutorService.class, false));
            }
        }
    }

    // 重点: 初始化ScheduledTaskRegistrar
    this.registrar.afterPropertiesSet();
}
```


### ScheduledTaskRegistrar

ScheduledAnnotationBeanPostProcessor默认使用的registrar是ScheduledTaskRegistrar
ScheduledTaskRegistrar初始化后,才会调度暂存的任务

```text
public ScheduledAnnotationBeanPostProcessor() {
    // 此时ScheduledTaskRegistrar还未初始化
    this.registrar = new ScheduledTaskRegistrar();
}

// ScheduledTaskRegistrar实例化后需要初始化
public class ScheduledTaskRegistrar implements ScheduledTaskHolder, InitializingBean, DisposableBean {}

public void afterPropertiesSet() {
    // 初始化时,开始调度暂存的任务
    scheduleTasks();
}

protected void scheduleTasks() {
    // 如果未设置taskScheduler,自己初始化
    if (this.taskScheduler == null) {
        this.localExecutor = Executors.newSingleThreadScheduledExecutor();
        this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
    }
    if (this.triggerTasks != null) {
        for (TriggerTask task : this.triggerTasks) {
            addScheduledTask(scheduleTriggerTask(task));
        }
    }
    if (this.cronTasks != null) {
        for (CronTask task : this.cronTasks) {
            addScheduledTask(scheduleCronTask(task));
        }
    }
    if (this.fixedRateTasks != null) {
        for (IntervalTask task : this.fixedRateTasks) {
            addScheduledTask(scheduleFixedRateTask(task));
        }
    }
    if (this.fixedDelayTasks != null) {
        for (IntervalTask task : this.fixedDelayTasks) {
            addScheduledTask(scheduleFixedDelayTask(task));
        }
    }
}
```

在ScheduledTaskRegistrar初始化完成前,调用ScheduledTaskRegistrar.scheduleCronTask方法,仅仅只将任务暂存起来.
```text
public ScheduledTask scheduleCronTask(CronTask task) {
    ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
    boolean newTask = false;
    if (scheduledTask == null) {
        scheduledTask = new ScheduledTask(task);
        newTask = true;
    }
    // 当taskScheduler设置时好时,直接调度任务;当taskScheduler未设置时好时,先暂存起来
    if (this.taskScheduler != null) {
        scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
    } else {
        addCronTask(task);
        this.unresolvedTasks.put(task, scheduledTask);
    }
    return (newTask ? scheduledTask : null);
}
```