# 多级缓存架构



## 1.什么是多级缓存

传统的缓存策略一般是请求到达Tomcat后，先查询Redis，如果未命中则查询数据库，如图：

![image-20210821075259137](MD图片/README.assets/image-20210821075259137.png)

存在下面的问题：

•请求要经过Tomcat处理，Tomcat的性能成为整个系统的瓶颈

•Redis缓存失效时，会对数据库产生冲击



多级缓存就是充分利用请求处理的每个环节，分别添加缓存，减轻Tomcat压力，提升服务性能：

- 浏览器访问静态资源时，优先读取浏览器本地缓存
- 访问非静态资源（ajax查询数据）时，访问服务端
- 请求到达Nginx后，优先读取Nginx本地缓存
- 如果Nginx本地缓存未命中，则去直接查询Redis（不经过Tomcat）
- 如果Redis查询未命中，则查询Tomcat
- 请求进入Tomcat后，优先查询JVM进程缓存
- 如果JVM进程缓存未命中，则查询数据库

![image-20210821075558137](MD图片/README.assets/image-20210821075558137.png)



在多级缓存架构中，Nginx内部需要编写本地缓存查询、Redis查询、Tomcat查询的业务逻辑，因此这样的nginx服务不再是一个**反向代理服务器**，而是一个编写**业务的Web服务器了**。



因此这样的业务Nginx服务也需要搭建集群来提高并发，再有专门的nginx服务来做反向代理，如图：

![image-20210821080511581](MD图片/README.assets/image-20210821080511581.png)



另外，我们的Tomcat服务将来也会部署为集群模式：

![image-20210821080954947](MD图片/README.assets/image-20210821080954947.png)



可见，多级缓存的关键有两个：

- 一个是在nginx中编写业务，实现nginx本地缓存、Redis、Tomcat的查询

- 另一个就是在Tomcat中实现JVM进程缓存

其中Nginx编程则会用到OpenResty框架结合Lua这样的语言。



这也是今天课程的难点和重点。







## 2.JVM进程缓存

缓存在日常开发中启动至关重要的作用，由于是存储在内存中，数据的读取速度是非常快的，能大量减少对数据库的访问，减少数据库的压力。我们把缓存分为两类：

- 分布式缓存，例如Redis：
  - 优点：存储容量更大、可靠性更好、可以在集群间共享
  - 缺点：访问缓存有网络开销
  - 场景：缓存数据量较大、可靠性要求较高、需要在集群间共享
- 进程本地缓存，例如HashMap、GuavaCache：
  - 优点：读取本地内存，没有网络开销，速度更快
  - 缺点：存储容量有限、可靠性较低、无法共享
  - 场景：性能要求较高，缓存数据量较小







### 使用Caffeine



**Caffeine**是一个基于Java8开发的，提供了近乎最佳命中率的高性能的本地缓存库。目前Spring内部的缓存使用的就是Caffeine。GitHub地址：https://github.com/ben-manes/caffeine

![image-20221212004734519](MD图片/README.assets/image-20221212004734519.png)

![image-20221212004755953](MD图片/README.assets/image-20221212004755953.png)







Caffeine的性能非常好，下图是官方给出的性能对比：

![image-20210821081826399](MD图片/README.assets/image-20210821081826399.png)

可以看到Caffeine的性能遥遥领先！[caffeine Wiki](https://github.com/ben-manes/caffeine/wiki/Benchmarks-zh-CN)



---



Caffeine既然是缓存的一种，肯定需要有缓存的清除策略，不然的话内存总会有耗尽的时候。

Caffeine提供了三种缓存驱逐策略：

- **基于容量**：设置缓存的数量上限

  ```java
  // 创建缓存对象
  Cache<String, String> cache = Caffeine.newBuilder()
      .maximumSize(1) // 设置缓存大小上限为 1
      .build();
  ```

- **基于时间**：设置缓存的有效时间

  ```java
  // 创建缓存对象
  Cache<String, String> cache = Caffeine.newBuilder()
      // 设置缓存有效期为 10 秒，从最后一次写入开始计时 
      .expireAfterWrite(Duration.ofSeconds(10)) 
      .build();
  
  ```

- **基于引用**：设置缓存为软引用或弱引用，利用GC来回收缓存数据。性能较差，不建议使用。

> **注意**：在默认情况下，当一个缓存元素过期的时候，Caffeine不会自动立即将其清理和驱逐。而是在一次读或写操作后，或者在空闲时间完成对失效数据的驱逐。



**使用 Caffeine 缓存基本API：**

导入坐标:

```xml
<!-- caffeine -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```



Tests:

```java
package com.ganga;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.jsqlparser.statement.select.KSQLWindow;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CaffeineTests {


    @Test
    void caffeineCacheTest() {

        //构建一个简单的 Cache 对象
        Cache<String, String> cache = Caffeine.newBuilder().build();

        //添加一个缓存
        cache.put("key1", "Ayaka");

        //获取缓存值 getIfPresent(key)
        //命中：返回值
        String key = cache.getIfPresent("key1");
        //未命中：返回 null
        String key2 = cache.getIfPresent("key2");
        System.out.println("key = " + key);
        System.out.println("key2 = " + key2);

        //使用 get(key,faction->{}) 未命中执行函数添加并返回缓存值
        key2 = cache.get("key2", value -> {
            //未命中查询数据库，执行业务逻辑
            //返回要写入缓存的值
            return "Ayaka的狗🌸";
        });

        System.out.println("key2 = " + key2);

    }


    /*
     基于大小设置驱逐策略：
     */
    @Test
    void testEvictByNum() throws InterruptedException {

        //构建Cache
        Cache<String, String> cache = Caffeine
                .newBuilder()
                .maximumSize(1) //最大缓存量
                .build();

        //写缓存
        cache.put("key1","Ayaka🌸");
        cache.put("key2","Ayaka520");
        cache.put("key3","Ayaka521");

        //等待
        Thread.sleep(20);

        //读缓存
        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));

    }


    /*
     基于时间设置驱逐策略：
     */
    @Test
    void testEvictByTime() throws InterruptedException {
        //构建Cache
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        //写缓存
        cache.put("key1","Ayaka🌸");
        cache.put("key2","Ayaka520");
        cache.put("key3","Ayaka521");

        //读缓存
        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));

        System.out.println("---------------");
        //等待
        Thread.sleep(1200L);
        //读缓存
        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));

    }
}
```





### 使用Caffeine实现JVM缓存





















## TODO