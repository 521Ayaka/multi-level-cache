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
