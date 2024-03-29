# Mysql

## 索引

MyISAM普通索引：索引顺序与数据行物理排列顺序无关

InnoDB聚集索引：聚集索引的顺序就决定了数据行的物理存储顺序,叶结点存储的是主键的值

1. **「主键索引」**(`PRIMARY KEY`)：**「唯一、非空」**
2. **「唯一索引」**(`UNIQUE`)：指定列不能出现重复数据；
3. **「前缀索引」**(`prefix INDEX`)：字符串有许多前缀重复，比如xx省xx市...；不能 order by 和 group by 
4. **「联合索引」**：一个列以上创建索引，减少开销，大量数据的表，使用联合索引会大大的减少开销。效率高。索引列越多，通过索引筛选出的数据越少。where和order by和group by**左前缀匹配原则**,遇到范围查找退化为线性查找。索引下推：命中第一个索引不回表直接筛选第二个条件的索引
5. **「覆盖索引」**：索引包含查询的所有字段的值，不用回表查询，**避免select ***出现回表查询.

**回表**：使用索引查询数据时，检索出来的数据可能包含其他列，但走的索引树叶子节点只能查到当前列值以及主键ID，所以需要根据主键ID再去查一遍数据，得到SQL 所需的列

**创建索引的策略**

- 不要在NULL值列上使用索引，尽量使用NOT NULL约束列上使用索引
- 很少查询的字段不要使用索引
- 大数据类型字段不创建索引
- 取值范围很小的列不创建索引？
- 索引对应的字段重复率太高，所以索引没用到，解决方法是建联合索引

**使用索引时的注意事项**

- 不要在条件NOT IN、<>、!= 等范围查询中使用索引
- 模糊查询时不要使用 %开头( 如 '%xxx' ,  '%xxx%')
- 查询索引的字段不要函数计算
- 联合索引查询时遵循最左原则，区分度最高的放在最左边
- 全部扫描超过30%不会走优化器；

[mysql 创建聚集索引_终于有人把MYSQL索引讲清楚了]:https://blog.csdn.net/weixin_39848007/article/details/111273706



## 优化

利用子查询优化**超多分页**场景。比如 limit offset , n 在MySQL是获取 offset + n的记录，再返回n条。而利用子查询则是查出n条，通过ID检索对应的记录出来，提高查询效率。

通过explain命令来查看SQL的执行计划，看看自己写的SQL是否走了索引，走了什么索引。通过show profile 来查看SQL对系统资源的损耗情况（不过一般还是比较少用到的）

在开启**事务**后，在事务内尽可能只操作数据库，并有意识地减少锁的持有时间（比如在事务内需要插入&&修改数据，那可以先插入后修改。因为修改是更新操作，会加行锁。如果先更新，那并发下可能会导致多个事务的请求等待行锁释放

查询慢如何优化

1. show profile查看具体是那条sql慢
2. explain查看sql执行计划，看自己写的sql是否走了索引
   1. possible_keys字段查看设置的索引
   2. key字段=实际用到的索引
   3. extra 会显示没走索引的操作?
      1. 看到filesort要警觉，会带来大量耗时，因为正常是索引排序是内存内排序
         1. orderby 可能会出现，排序字段没有在索引内
         2. 建一个索引，既能满足查询又能排序，比如是先where id,orderby time,建一个id在前，time在后的索引
      2. using mmr是主键排序了，如果查询的条件字段本身有序，可以置顶order这个字段，取消主键排序
      3. using where 代表扫描数据按行帅选了
   4. 看type是否是all进行了全表查询，
      1. const 通过常量值帅选出唯一数据，效率高
      2. range 代表通过索引进行了范围扫描 where id>3
   5. rows字段扫描行数
3. 检查是否用了最优索引
4. 是否查询了过多字段或过多数据
5. 分表
6. 不等于操作符是永远不会用到索引的，因此对它的处理只会产生全表扫描。 优化方法： key<>0 改为 key>0 or key<0。
7. 开启慢查询l日志；例如100ms外
8. or 语句前后没有同时使用索引。当 or 语句查询字段只有一个是索引，该索引失效，只有当 or 语句左右查询字段均为索引时，才会生效
9. 数据类型出现隐式转化，那么会导致索引失效，造成全表扫描效率极低
10. 对于复合索引，如果不使用前列，后续列也将无法使用

**旧数据**存到hive

有**字符串检索**场景把数据转移到Elastic Search：MySQL->Elasticsearch需要有对应的同步程序(一般就是监听MySQL的binlog，解析binlog后导入到Elasticsearch)

根据查询条件的维度，做相对应的聚合表，线上的请求就查询聚合表的数据，不走原表。比如订单表按天聚合

写有性能瓶颈，升级主从架构，读写分离。还有瓶颈可以分库分表

分库分表一般可以按userid，按照用户的维度查询比较多。主键基于雪花算法或redis的较多。

**分库分表迁移过程**：双写：

1. 增量的消息各自往新表和旧表写一份 
2. 将旧表的数据迁移至新库
3. 迟早新表的数据都会追得上旧表（在某个节点上数据是同步的）
4. 校验新表和老表的数据是否正常（主要看能不能对得上）
5. 开启双读（一部分流量走新表，一部分流量走老表），相当于灰度上线的过程
6. 读流量全部切新表，停止老表的写入
7. 提前准备回滚机制，临时切换失败能恢复正常业务以及有修数据的相关程序。

**减少逻辑运算量**

- **避免使用函数**，将运算转移至易扩展的应用服务器中
   如substr等字符运算，dateadd/datesub等日期运算，abs等数学函数
- **减少排序**，利用索引取得有序数据或避免不必要排序
   如union all代替 union，order by 索引字段等
- **禁止类型转换**，使用合适类型并保证传入参数类型与数据库字段类型绝对一致
   如数字用tiny/int/bigint等，必需转换的在传入数据库之前在应用中转好
- **简单类型**，尽量避免复杂类型，降低由于复杂类型带来的附加运算。更小的数据类型占用更少的磁盘、内存、cpu缓存和cpu周期

**减少逻辑IO量**

- **index**，优化索引，减少不必要的表扫描
   如增加索引，调整组合索引字段顺序，去除选择性很差的索引字段等等
- **table**，合理拆分，适度冗余
   如将很少使用的大字段拆分到独立表，非常频繁的小字段冗余到“引用表”
- **SQL**，调整SQL写法，充分利用现有索引，避免不必要的扫描，排序及其他操作
   如减少复杂join，减少order by，尽量union all，避免子查询等
- **数据类型**，够用就好，减少不必要使用大字段
   如tinyint够用就别总是int，int够用也别老bigint，date够用也别总是timestamp



# Spring	

**IOC**容器解决对象管理和对象依赖的问题。**依赖注入**在我的理解下，是**控制反转**的实现方式为什么不New对象，好处是：将对象集中统一管理」并且「降低耦合度」类似工厂模式。方便 单元测试、对象创建复杂、对象依赖复杂、单例等。还有一整套的Bean生命周期管理，实现对象增强，AOP。

**AOP** 解决的是 非业务代码抽取的问题。监控客户端进行封装。控基本的指标有QPS、RT、ERROR等等。注解+AOP的方式封装。Spring内的事务传播行为，需要传播的方法是AOP代理对象。



# Java

## 基本

基本数据类型，`==` 比较值，引用数据类型`==` 比较内存地址(equals()默认)。

`hashCode()` 的作用是获取哈希码（`int` 整数）（本地方法c++）通常用来将对象的内存地址转换为整数之后返回。如果两个对象的`hashCode` 值相等，那这两个对象不一定相等（哈希碰撞）。重写 ``equals()`` 时必须重写 ``hashCode() ``方法。

`hashCode()`的默认行为是对堆上的对象产生独特值。如果没有重写 `hashCode()`，则该 class 的两个对象无论如何都不会相等（即使这两个对象指向相同的数据）。

基本数据类存放在栈中，对象实例存在堆。

StringBuffer线程安全，StringBuilder非线程安全。`+和+=`在String上是由StringBuilder.append()后toString()实现，但会创建多个StringBuilder对象。字符串常量池避免字符串的重复创建，在堆中。

**代理**:使用代理对象来代替对真实对象,不修改原目标对象的前提下，提供额外的功能操作，扩展目标对象的功能

浮点数之间的等值判断，基本数据类型不能用==来比较，包装数据类型不能用 equals 来判断。

使用它的`BigDecimal(String val)`构造方法或者 `BigDecimal.valueOf(double val)` 静态方法来创建对象。

## 集合

**判断所有集合内部的元素是否为空，使用 `isEmpty()` 方法，而不是 `size()==0` 的方式。**

### Map

**HashMap**

链表长度大于阈值（默认为 8）（将链表转换成红黑树前会判断，如果当前数组的长度小于 64，那么会选择先进行数组扩容，而不是转换为红黑树）时，将链表转化为红黑树，以减少搜索时间

Hashtable 已经淘汰

**LinkedHashMap**:上面结构的基础上，增加了一条双向链表,保证了顺序。

**ConcurrentHashMap**保证了线程安全



### List

**ArrayList** ：出现快速失败的前提是使用了iterator进行遍历修改数据或多线程修改数据，换用`java.util.concurrent.CopyOnWriteArrayList`

**LinkedList** 双向链表

`Vector` 是 `List` 的古老实现类，底层使用`Object[ ]` 存储，线程安全的。

### Set

`HashSet` 用于不需要保证元素插入和取出顺序的场景，添加对象时先`hashcode()`若相同进一步判断`equals`()

`LinkedHashSet` 用于保证元素的插入和取出顺序满足 FIFO 的场景，`TreeSet`(红黑树) 用于支持对元素自定义排序规则的场景。

## 线程

锁 

synchronized:  保证被它修饰的方法或者代码块在任意时刻只能有一个线程执

volatile 

线程池 参数是描述等待队列，成无界的有可能出现OOM

ThreadLocal:每个线程绑定自己的值

内存泄漏

## 异常

尽量缩小try...catch的范围，finally从句里放释放资源，先用IOException等专业异常处理，再用Exception兜底

## JVM虚拟机和内存调优

现系统很卡日志里频繁出现OOM异常，用dump文件看OOM时的内存镜像，看的工具可以是JMAT，通过dump文件，再结合日志上下文。原因可能：ThreadLocal里的对象用好没remove（弱引用），Redis缓存超时时间过长，创建线程池时，等待队列设置成了无界，数据库查询太多记录

[美团GC优化](https://tech.meituan.com/2017/12/29/jvm-optimize.html)

###  Java 内存区域

**程序计数器**:线程切换后能恢复到正确的执行位置，每条线程都需要有一个独立的程序计数器,是唯一一个不会出现 ``OutOfMemoryError`` 的内存区域

**Java 虚拟机栈**:线程私有的,线程请求栈的深度超过当前 Java 虚拟机栈的最大深度的时候，就抛出`` StackOverFlowError ``错误。栈的内存大小可以动态扩展， 如果虚拟机在动态扩展栈时无法申请到足够的内存空间，则抛出`OutOfMemoryError`异常。

本地方法栈：线程私有，本地方法栈则为虚拟机使用到的 Native 方法服务。也会出现 `StackOverFlowError` 和` OutOfMemoryError `两种错误。

#### 堆

线程共享的一块内存区域，在虚拟机启动时创建。**此内存区域的唯一目的就是存放对象实例，几乎所有的对象实例以及数组都在这里分配内存。**

**新生代**（Young Generation）

一个Eden区，两个Survivor区。大部分对象在Eden区中生成。当Eden区满时，还存活的对象将被复制到两个Survivor区（中的一个）

Eden S0 S1



**老年代**（Old Generation）：在新生代中经历了N次垃圾回收后仍然存活的对象，就会被放到年老代，整堆包括新生代和老年代的垃圾回收称为Full GC。





Metaspace(元空间)：元空间使用的是直接内存,与垃圾回收要回收的Java对象关系不大。



### 简单调优

开发过程中，通常会将 -Xms 与 -Xmx两个参数配置成相同的值，其目的是为了能够在java垃圾回收机制清理完堆区后不需要重新分隔计算堆区的大小而浪费资源。

根据实际事情调整新生代和幸存代的大小，官方推荐新生代占java堆的3/8，幸存代占新生代的1/10

### GC日志

### 对象创建

1. 类加载检查
2. 分配内存
3. 初始化零值
4. 设置对象头
5. 执行init方法







# 结合

幂等=工人刷卡计件

## xxl涉及模块

大屏，看板：table滚动：Pagination直接复制current，定时器滚动进度条，满了倒着走

每天款式工序视频文件上传，ai图上传记录文件md5（客户端是wpf，web端只负责接口）

ai图转pdf并且在线显示

旧罗斯系统转移数据到新系统(或者说是只有数据库，另外做一套罗斯数据查询系统)

扫码计件api接口

出库入库,发裁片api接口

密集柜语音控制api接口

微信公众号

数据库行转列：衣服尺码size1-size20，(一个款号)行记录是尺码号，转成170，180列。

每天（月）定时任务计算漏扫码

扫码补录权限问题

总检工序疵点记录

打印二维码lodop

erp系统买的通伟服装

gitea

BigDecimal算计件工资

阿里云oss（原本放在公司公网服务器，后面转移到阿里云oss,方便设计师上传ai文件，样衣组下载）

elasticSearch用来查询员工扫码记录，数据量大。存放卡号对应款号，款式颜色等信息。（在生产的款号信息存入redis）,先日志记录计算qps

衣服的卡号二维码是aes加密的

工人刷卡(幂等)，进行查询款式信息可能涉及多个模块，使用异步来获取(feign+异步)，工人刷卡加入消息队列，保证消息可用，刷卡失败后，异常处理刷卡数据



江西厦门两地数据库同步）

厦门断电服务器关机的异地容灾（21年十月或九月，五六七三天，七厦门完全断电，江西上班）；这个需要微服务和数据库都部署多份

把兴学来的系统拆分一下数据库和模块设计。

测算每天扫码的qps，数据库和es的数据量，jmeter做压力测试，nginx动静分离，废话vue的前端

msql的逻辑删除  mybatis,一级二级缓存

# 面试

在回答好当前提问后多说一句，继续引导面试官提问。

[面试技巧](https://www.zhihu.com/question/452184164/answer/2260444337)



# 待

OOM调优

分布式日志：ELK EFK



maven install package区别

- install **本地仓库**的相应目录中，供其他项目或模块引用
- package就生成jar

springboot 三级缓存

线程池相关

mysql多条语句查看那条慢profile，complain

mysql索引失效的情况

hashtable，vetor为什么过时

springboot的bean是否线程安全，怎么使得bean线程安全

threadlocal

>  Spring的作用域(scope)：
>
> singleton:单例，默认作用域。
>
> prototype:原型，每次创建一个新对象。
>
> request:请求，每次Http请求创建一个新对象，适用于WebApplicationContext环境下。
>
> session:会话，同一个会话共享一个实例，不同会话使用不用的实例。
>
> global-session:全局会话，所有会话共享一个实例。

gataway用处

nacos配置中心改了数据库链接，生效？：

druid可以  配置动态数据源，

如何切换dev环境，不用手动输入参数

redis用过哪些数据结构

threadlocal为什么会泄露：

- 两个原因，web请求线程池在并发中可能用到同一个线程，
- key是弱引用，记得收容remove

线程顺序执行

- 使用join，父线程等待子线程结束之后才能继续运行
- 使用单线程的线程池
- volatile做一个信号量

线程间共享变量

- synchronized 枷锁
- volatile 是访问可见（强迫线程重新读取变量值）无法保证原子性（i++）
- 使用atomic 包小的atomicnintger等



怎么做鉴权的feign

feign如何返回异常报错信息

全局异常

dockerfile怎么做

springboot @scoop

如何在springboot中输出启动端口

mysql 慢查询日志

mysql和redis一致性 ，mq异步通知不断重试，保证消息顺序一致性，canal监听log同步到mq.然后同步到redis，分布式锁，

延迟双删：删redis-更新sql-延迟n秒删redis（sleep延迟对性能不好，可以用mq延迟消息）

wait和sleep区别

sleep（0）可以主动让出cpu（阻塞态）

thread.yield() 变为就绪态

降级和熔断区别

- 熔断一般是故障引起
- 降级一般从整体负荷考虑

redis 大key，直接删除消耗资源，用unlink一部删除。bigkes查找string类型大key

如何查看springboot的 mapping（请求接口），数据库连接情况

springboot的事务注解的传播行为：

事务失效可能：

	1. 抛出受检异常，可以设置for
	1. 多线程调用，因为数据库连接是threadlocal内

1. service 一个方法用this调用本类的事务方法，因为this对象不是代理对象，所以导致事务失效
   1. 可以使用aopcontext.currentProxy（）强转后获得代理对象，然后调用事务方法

nacao挂了其他服务正常通讯，每次服务上线注册自己，然后拉去所有在线

实际优化的sql：

spring注解

vue生命周期

mybatisplus wapper怎么写

使用的springcloud版本



# .NET

**使用泛型的好处？**
●代码复用：我们一套代码可以支持不同的类性。
●降低了耦合性：代码逻辑和数据类型之间分离，实现了解耦。
●更好的可读性：我们在使用集合的时候，定义了一个list 如List<String>，一看便知道这个一个存放String类型的list。
●程序性能提高：在一定的程度上减少了类型转换装箱与拆箱操作带来的性能损耗。

**C#下Hashtable和Dictionary区别**

- Dictionary是泛型存储，不需要进行类型转换，Hashtable由于使用object，在存储或者读取值时都需要进行类型转换，所以比较耗时。

- 单线程程序中推荐使用 Dictionary, 有泛型优势, 且读取速度较快, 容量利用更充分。多线程程序中推荐使用 Hashtable, 默认的 Hashtable 允许单线程写入, 多线程读取, 对 Hashtable 进一步调用 Synchronized() 方法可以获得完全线程安全的类型. 而 Dictionary 非线程安全, 必须人为使用 lock 语句进行保护, 效率大减。

  

**简述后台线程和前台线程的区别？**

应用程序必须运行完所有的前台线程才可以退出；
而对于后台线程，应用程序则可以不考虑其是否已经运行完毕而直接退出，所有的后台线程在应用程序退出时都会自动结束。
通过将 Thread.IsBackground 设置为 true，就可以将线程指定为后台线程，主线程就是一个前台线程。

**C#四种委托的异同？**
1delegate ,至少0个参数，至多32个参数，可以无返回值，可以指定返回值类型。
2Action ,至少0个参数，无返回值的泛型委托。
3Func ,至少0个参数，至多16个参数，必须有返回值的泛型委托。
4Predicate ,有且只有一个参数，返回值只为 bool 类型。

**ADO.NET常用的对象有哪些，分别有什么作用？**

SqlConnection :连接对象，用于执行与数据库的连接。 
SqlCommand:命令对象，用于对数据库执行 SQL 语句。 
SqlDataAdapter:适配器对象，用于填充数据集和更新数据库。 
SqlParameter:参数对象，用于执行参数化 SQL 语句。 
SqlDataReader:读取器对象，用于从数据库中快速逐行读取数据。 SqlTransaction:事务对象，用于执行数据库事务。

**C#中堆和栈的区别？**

栈：由编译器自动分配、释放。在函数体中定义的变量通常在栈上。堆：一般由程序员分配释放。用 new、 malloc 等分配内存函数分配得到的就是在堆上。存放在栈中时要管存储顺序，保持着先进后出的原则，他是一片连续的内存域，有系统自动分配和维护；

**静态构造函数特点是什么**？

最先被执行的构造函数，且在一个类里只允许有一个无参的静态构造函数

执行顺序：静态变量 > 静态构造函数 > 实例变量 > 实例构造函数

**C#中什么是值类型与引用类型？**

值类型：struct 、 enum 、 int 、 float 、 char 、 bool 、 decimal

引用类型：class 、 delegate 、 interface 、 array 、 object 、 string

**请详述在C#中类(class)与结构(struct)的异同？**

class 可以被实例化 , 属于引用类型 ,

class 可以实现接口和单继承其他类 , 还可以作为基类型 , 是分配在内存的堆上的

struct 属于值类型 , 不能作为基类型 , 但是可以实现接口 , 是分配在内存的栈上的 .

**C#中参数传递 ref 与 out 的区别？**

ref 指定的参数在函数调用时必须先初始化，而 out 不用

 out 指定的参数在进入函数时会清空自己，因此必须在函数内部进行初始化赋值操作，而 ref 不用

总结：ref 可以把值传到方法里，也可以把值传到方法外；out 只可以把值传到方法外

**C#中用sealed修饰的类有什么特点？**

密封，不能继承。

**什么是扩展方法**？

必须要静态类中的静态方法 2. 第一个参数的类型是要扩展的类型，并且需要添加this 关键字以标识其为扩展方法

**const和readonly有什么区别？**

1 、初始化位置不同。const 必须在声明的同时赋值；readonly 即可以在声明处赋值 ;

2 、修饰对象不同。const 即可以修饰类的字段，也可以修饰局部变量；readonly 只能修饰类的字段

3 、 const 是编译时常量，在编译时确定该值；readonly 是运行时常量，在运行时确定该值。

4 、 const 默认是静态的；而 readonly 如果设置成静态需要显示声明

5 、修饰引用类型时不同， const 只能修饰 string 或值为 null 的其他引用类型；readonly 可以是任何类型。

Math.Round 将值舍入到最接近的整数或指定的小数位数。

**序列化**

使用`BinaryFormatter`，被序列化的对象的类上`[Serializable]`

**反射**

在使用反射的开始，你会获取一个 Type 类型的对象，从这个对象上进一步获取 程序集，类型，模块 等信息，可以通过 反射 动态的生成某个类型的实例，甚至还能动态调用这个类型上的方法。

Assembly定义和加载程序集

MethodInfo了解方法的名称

FieldInfo了解字段的名称

PropertyInfo了解属性的名称

反射创建对象

根据程序集名

`assembly.CreateInstance`

根据类型

`Activator.CreateInstance`

**修饰符**

public：成员可以被任何代码访问。

private：成员仅能被同一个类中的代码访问，如果在类成员前未使用任何访问修饰 符，则默认为private。

 internal：成员仅能被同一个项目中的代码访问。

protected：成员只能由类或派生类中的代码访问

**解释什么是依赖属性，它和以前的属性有什么不同？为什么在WPF会使用它？**

(1)依赖属性是一种特定类型的属性。这种属性的特殊之处在于，其属性值受到 Windows 运行时中专用属性系统的跟踪和影响。

(2)依赖属性的用途是提供一种系统的方式，用来基于其他输入（在应用运行时其内部出现的其他属性、事件和状态）计算属性的值。

(3)依赖属性代表或支持编程模型的某种特定功能，用于定义 Windows 运行时应用，这种模型使用 XAML 编写 UI，使用 C#、Microsoft Visual Basic 或 Visual C++ 组件扩展 (C++/CX) 编写代码。

 **Winform中如何跨线程修改控件**

通过，在这个子线程中，通过this.Invoke() 或 this.BeginInvoke()的方式通过执行委托的方式，在委托里去修改，就可以。

**遍历页面上所有TextBox控件**

`this.Controls`

**怎么让一个窗体在运行时，只能打开一个？**

遍历Application的OpenedForms集合，从中如果找到了该Form，将其激活即可

 **如何在关闭窗体时，可以取消关闭？**

Form_Closing事件中，首先显示询问消息框，当用户点击“是”才执行关闭，当点击“否”，

 **如何在Form加载时，动态添加控件到Form中？**

this.Controls.Add

## Task

**启动方式**

``` C#
　　var t1 = new Task(() => TaskMethod("Task 1"));
　　t1.Start();
　　Task.WaitAll(t1);//等待所有任务结束 　　注:任务的状态:

   Task.Factory.StartNew(() => TaskMethod("Task 3")); 直接异步的方法
　　//或者
　　var t3=Task.Factory.StartNew(() => TaskMethod("Task 3"));
　　Task.WaitAll(t3);//等待所有任务结束

//主线程阻塞，等待结束
Task.WaitAll(taskList.ToArray());

  //带返回值
　  Task<int> task = CreateTask("Task 1");
　　task.Start();
　　int result = task.Result;

//任务完成时执行处理   Task cwt = task.ContinueWith

//使用IProgress实现异步编程的进程通知
 progress.Report(1);


//取消任务，任务中遍历 tokenSource.IsCancellationRequested
```

**取消任务**

` var cts = new CancellationTokenSource();`

　ThreadPool相比Thread来说具备了很多优势，但是ThreadPool却又存在一些使用上的不方便。比如：
　　◆ ThreadPool不支持线程的取消、完成、失败通知等交互性操作；
　　◆ ThreadPool不支持线程执行的先后次序；

## Sql Server

行转列 Case When，PIVOT

主键做聚集索引，一般来说，存储的顺序和索引的顺序一致

非聚集索引，存储的顺序和索引不是按顺序的

PS：聚集索引一个表只能有一个，而非聚集索引一个表可以存在多个。

 **存储过程**

Create Procedur name @xxx nvchar(50)  @xxx char()output as  begin end ,

## 数据类型

**varchar和nvarchar的区别**

Unicode字符集是为了解决字符集这种不兼容的问题而产生的，它所有的字符都用两个字节表示，即英文字符也是用两个字节表示。

中文字符，用nchar/nvarchar存储，如果纯英文和数字(保证不含中文)，则用char/varchar存储。

**sql左连接、右连接、内连接有什么区别？**

1. 左连接：左连接的基础表为left join左侧数据表，右表只会展示符合搜索条件的记录。
2. 右连接：右连接的基础表为right join右侧数据表，左表只会展示符合搜索条件的记录，左表不足的地方用null填充。
3. 内连接：并不以谁为基础,它只显示符合条件的记录



group by 单独使用可以去重