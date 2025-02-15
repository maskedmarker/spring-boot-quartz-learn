

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
