# quartz更细节的实现

```text
Configuration of Quartz is typically done through the use of a properties file, in conjunction with the use of StdSchedulerFactory.
Alternatively, you can explicitly initialize the factory by calling one of the initialize(xx) methods before calling getScheduler() on the StdSchedulerFactory.

Instances of the specified JobStore, ThreadPool, and other SPI classes will be created by name, 
and then any additional properties specified for them in the config file will be set on the instance by calling an equivalent ‘set’ method. 
For example if the properties file contains the property ‘org.quartz.jobStore.myProp=10’ then after the JobStore class has been instantiated, 
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
    String requestedFile=System.getProperty(PROPERTIES_FILE);
    String propFileName=requestedFile != null ? requestedFile : "quartz.properties";
    // ...
    Properties props=new Properties();
    // ...
    InputStream in=null;
    in=cl.getResourceAsStream("quartz.properties");
    if (in == null) {
        in=cl.getResourceAsStream("/quartz.properties");
    }
    if (in == null) {
        in=cl.getResourceAsStream("org/quartz/quartz.properties");
    }
    props.load(in);
    // ...

    // 系统参数优先于properties文件
    initialize(overrideWithSysProps(props, getLog()));
}
```
org.quartz.impl.StdSchedulerFactory.instantiate()对Scheduler的子类进行实例化和初始化.



### JobStore
JobStore’s are responsible for keeping track of all the “work data” that you give to the scheduler: jobs, triggers, calendars, etc. 
Selecting the appropriate JobStore for your Quartz scheduler instance is an important step.
You can declare which JobStore your scheduler should use (and it’s configuration settings) in the properties file (or object) that you provide to the SchedulerFactory that you use to produce your scheduler instance.

```text
Never use a JobStore instance directly in your code. For some reason many people attempt to do this. 
The JobStore is for behind-the-scenes use of Quartz itself. 
You have to tell Quartz (through configuration) which JobStore to use, but then you should only work with the Scheduler interface in your code.
```

#### RAMJobStore

RAMJobStore gets its name in the obvious way: it keeps all of its data in RAM.
The drawback is that when your application ends (or crashes) all of the scheduling information is lost - this means RAMJobStore cannot honor the setting of “non-volatility” on jobs and triggers.
缺点是当你的应用程序结束（或者崩溃）时,所有的调度信息都会丢失——这意味着RAMJobStore无法尊重作业和触发器的‘非易失性’设置.

#### JDBCJobStore

JDBCJobStore is also aptly named - it keeps all of its data in a database via JDBC.
To use JDBCJobStore, you must first create a set of database tables for Quartz to use.
You can find table-creation SQL scripts in the “docs/dbTables” directory of the Quartz distribution.

all the tables start with the prefix “QRTZ_”.
This prefix can actually be anything you’d like, as long as you inform JDBCJobStore what the prefix is (in your Quartz properties). 
Using different prefixes may be useful for creating multiple sets of tables, for multiple scheduler instances, within the same database.

##### transaction
Once you’ve got the tables created, you have one more major decision to make before configuring and firing up JDBCJobStore. 
**You need to decide what type of transactions your application needs.** 
If you don’t need to tie your scheduling commands (such as adding and removing triggers) to other transactions, then you can let Quartz manage the transaction by using JobStoreTX as your JobStore (this is the most common selection).
If you need Quartz to work along with other transactions (i.e. within a J2EE application server), then you should use JobStoreCMT - in which case Quartz will let the app server container manage the transactions.

##### datasource
The last piece of the puzzle is setting up a DataSource from which JDBCJobStore can get connections to your database.
DataSources are defined in your Quartz properties using one of a few different approaches. 
One approach is to have Quartz create and manage the DataSource itself - by providing all of the connection information for the database. 
Another approach is to have Quartz use a DataSource that is managed by an application server that Quartz is running inside of - by providing JDBCJobStore the JNDI name of the DataSource.


#### Configuring Quartz to use JobStoreTx

you need to select a DriverDelegate for the JobStore to use.
The DriverDelegate is responsible for doing any JDBC work that may be needed for your specific database. 
StdJDBCDelegate is a delegate that uses “vanilla”(标准的) JDBC code (and SQL statements) to do its work.
Next, you need to inform the JobStore what table prefix (discussed above) you are using.
And finally, you need to set which DataSource should be used by the JobStore.

```text
org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.tablePrefix=QRTZ_
org.quartz.jobStore.dataSource=myDS

dataSource的配置如下
org.quartz.dataSource.myDS.driver=
org.quartz.dataSource.myDS.URL=
org.quartz.dataSource.myDS.user=
org.quartz.dataSource.myDS.password=
org.quartz.dataSource.myDS.maxConnections=
org.quartz.dataSource.myDS.validationQuery=
org.quartz.dataSource.myDS.idleConnectionValidationSeconds=
org.quartz.dataSource.myDS.validateOnCheckout=
```

备注: 
```text
The "org.quartz.jobStore.useProperties" config parameter can be set to "true" (defaults to false) in order to instruct JDBCJobStore that all values in JobDataMaps will be Strings, 
and therefore can be stored as name-value pairs, rather than storing more complex objects in their serialized form in the BLOB column. 
This is much safer in the long term, as you avoid the class versioning issues that there are with serializing your non-String classes into a BLOB.
```



#### TerracottaJobStore

TerracottaJobStore provides a means for scaling and robustness without the use of a database.
TerracottaJobStore can be ran clustered or non-clustered, and in either case provides a storage medium for your job data that is persistent between application restarts, because the data is stored in the Terracotta server.


##### Terracotta server

Terracotta是一种透明的,实现jvm cluster的技术.
Terracotta server用来存储多个jvm实例的共享变量.

关于Terracotta server的介绍: https://www.infoq.com/articles/open-terracotta-intro/
```text
In this article, we introduce OpenTerracotta, an enterprise-class, open-source JVM-level clustering solution.
JVM-level clustering simplifies enterprise Java by enabling applications to be deployed on multiple JVMs, yet interact with each other as if they were running on the same JVM.

Open Terracotta allows threads in a cluster of JVMs to interact with each other across JVM boundaries using the same built-in JVM facilities extended to have a cluster-wide meaning. 
These clustering capabilities are injected into the bytecode of the application classes at runtime, so there is no need to code to a special clustering API.
```


### cluster

quartz的集群模式需要使用到Terracotta技术.

```text
Clustering currently works with the JDBC-Jobstore (JobStoreTX or JobStoreCMT) and the TerracottaJobStore. 
Features include load-balancing and job fail-over (if the JobDetail’s “request recovery” flag is set to true).


Clustering With JobStoreTX or JobStoreCMT Enable clustering by setting the “org.quartz.jobStore.isClustered” property to “true”. 
Each instance in the cluster should use the same copy of the quartz.properties file. 
Exceptions of this would be to use properties files that are identical, with the following allowable exceptions: Different thread pool size, and different value for the “org.quartz.scheduler.instanceId” property. Each node in the cluster MUST have a unique instanceId, which is easily done (without needing different properties files) by placing “AUTO” as the value of this property.


Never run clustering on separate machines, unless their clocks are synchronized using some form of time-sync service (daemon) that runs very regularly (the clocks must be within a second of each other). 


Never fire-up a non-clustered instance against the same set of tables that any other instance is running against. 
You may get serious data corruption, and will definitely experience erratic behavior.
```
