package com.ganga.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class CaffeineContent {

    public static final String CACHE_ITEM_ID_KEY = "item:id:";

    public static final String CACHE_STOCK_ID_KEY = "stock:id:";

    public static final String CACHE_ITEM_PAGE_KEY = "item:page:";

    /**
     * 通过 key 获取 id
     * @param key
     * @return
     */
    public static Long KEY(String key) {
        List<String> split = StrUtil.split(":", key);
        if (ObjectUtils.isEmpty(split)){
            return null;
        }
        return Long.valueOf(split.get(split.size() - 1));
    }

    /**
     * 通过 key 获取分页参数
     * @param key
     * @return
     */
    public static Long[] KEYS(String key){
        List<String> split = StrUtil.split(":", key);
        if (ObjectUtils.isEmpty(split)){
            return null;
        }

        return new Long[]{Long.valueOf(split.get(split.size()-2)), Long.valueOf(split.get(split.size()-1))};
    }

}
