# quartz更细节的实现

```text
Configuration of Quartz is typically done through the use of a properties file, in conjunction with the use of StdSchedulerFactory.
Alternatively, you can explicitly initialize the factory by calling one of the initialize(xx) methods before calling getScheduler() on the StdSchedulerFactory.

Instances of the specified JobStore, ThreadPool, and other SPI classes will be created by name, 
and then any additional properties specified for them in the config file will be set on the instance by calling an equivalent ‘set’ method. 
For example if the properties file contains the property ‘org.quartz.jobStore.myProp = 10’ then after the JobStore class has been instantiated, 
the method ‘setMyProp()’ will be called on it. 
比如
Scheduler的配置,可以参考: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigMain.html
ThreadPool的配置,可以参考: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigThreadPool.html
```



### StdSchedulerFactory
初始化加载配置项
```text
org.quartz.impl.StdSchedulerFactory.initialize()

public void initialize() throws SchedulerException {
    // ...
    String requestedFile = System.getProperty(PROPERTIES_FILE);
    String propFileName = requestedFile != null ? requestedFile : "quartz.properties";
    // ...
    Properties props = new Properties();
    // ...
    InputStream in = null;
    in = cl.getResourceAsStream("quartz.properties");
    if (in == null) {
        in = cl.getResourceAsStream("/quartz.properties");
    }
    if (in == null) {
        in = cl.getResourceAsStream("org/quartz/quartz.properties");
    }
    props.load(in);
    // ...

    // 系统参数优先于properties文件
    initialize(overrideWithSysProps(props, getLog()));
}
```
org.quartz.impl.StdSchedulerFactory.instantiate()对Scheduler的子类进行实例化和初始化.



### 
