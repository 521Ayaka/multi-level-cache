package com.ganga.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ganga.pojo.Item;
import com.ganga.pojo.ItemStock;
import com.ganga.pojo.PageDTO;
import com.ganga.service.IItemService;
import com.ganga.service.IItemStockService;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.ganga.utils.CaffeineContent.*;

@RestController
@RequestMapping("item")
public class ItemController {

    @Autowired
    private IItemService itemService;
    @Autowired
    private IItemStockService stockService;

    @Autowired
    private Cache<String, Item> itemCache;
    @Autowired
    private Cache<String, ItemStock> itemStockCache;
    @Autowired
    private Cache<String, Page<Item>> itemListCache;

    @GetMapping("list")
    public PageDTO queryItemPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {

        //通过缓存查询
        Page<Item> result = itemListCache.get(CACHE_ITEM_PAGE_KEY + page + ":" + size,
                key -> {
                    // 分页查询商品
                    return itemService.query()
                            .ne("status", 3)
                            .page(new Page<>(page, size));
                });
        if (ObjectUtils.isEmpty(result)){
            return null;
        }

        // 查询库存 返回数据
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
        return itemCache.get(CACHE_ITEM_ID_KEY + id,
                key -> itemService.query()
                        .ne("status", 3).eq("id", id)
                        .one()
        );
    }

    @GetMapping("/stock/{id}")
    public ItemStock findStockById(@PathVariable("id") Long id) {
        return itemStockCache.get(CACHE_STOCK_ID_KEY + id, key->stockService.getById(id));
    }

}
