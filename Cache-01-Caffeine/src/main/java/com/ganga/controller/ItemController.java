package com.ganga.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ganga.pojo.Item;
import com.ganga.pojo.ItemStock;
import com.ganga.pojo.PageDTO;
import com.ganga.service.IItemService;
import com.ganga.service.IItemStockService;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("item")
public class ItemController {

    @Autowired
    private IItemService itemService;
    @Autowired
    private IItemStockService stockService;

    @Autowired
    private Cache<Long, Item> itemCache;
    @Autowired
    private Cache<Long, ItemStock> itemStockCache;

    @GetMapping("list")
    public PageDTO queryItemPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        // 分页查询商品
        Page<Item> result = itemService.query()
                .ne("status", 3)
                .page(new Page<>(page, size));

        // 查询库存
        List<Item> list = result.getRecords().stream().peek(item -> {
            ItemStock stock = stockService.getById(item.getId());
            item.setStock(stock.getStock());
            item.setSold(stock.getSold());
        }).collect(Collectors.toList());

        // 封装返回
        return new PageDTO(result.getTotal(), list);
    }

    @PostMapping
    public void saveItem(@RequestBody Item item) {
        itemService.saveItem(item); //TODO: Cache synchronization
    }

    @PutMapping
    public void updateItem(@RequestBody Item item) {
        itemService.updateById(item); //TODO: Cache synchronization
    }

    @PutMapping("stock")
    public void updateStock(@RequestBody ItemStock itemStock) {
        stockService.updateById(itemStock); //TODO: Cache synchronization
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable("id") Long id) {
        boolean isSuccess = itemService.update().set("status", 3).eq("id", id).update();
        //TODO: Cache synchronization
    }

    @GetMapping("/{id}")
    public Item findById(@PathVariable("id") Long id) {
        return itemCache.get(id,
                key -> itemService.query()
                        .ne("status", 3).eq("id", key)
                        .one()
        );
    }

    @GetMapping("/stock/{id}")
    public ItemStock findStockById(@PathVariable("id") Long id) {
        return itemStockCache.get(id, key->stockService.getById(id));
    }

}
