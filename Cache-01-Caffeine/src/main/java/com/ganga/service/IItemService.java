package com.ganga.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ganga.pojo.Item;

public interface IItemService extends IService<Item> {
    void saveItem(Item item);
}
