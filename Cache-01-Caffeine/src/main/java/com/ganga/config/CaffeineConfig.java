package com.ganga.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ganga.pojo.Item;
import com.ganga.pojo.ItemStock;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CaffeineConfig {

    /**
     * @return 商品缓存对象
     */
    @Bean
    public Cache<String, Item> itemCache(){
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_00)
                .build();
    }

    /**
     * @return 库存缓存对象
     */
    @Bean
    public Cache<String, ItemStock> itemStockCache(){
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_000)
                .build();
    }

    /**
     * @return 商品分页缓存
     */
    @Bean
    public Cache<String, Page<Item>> itemListCache(){
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(5_000)
                .build();
    }


}

