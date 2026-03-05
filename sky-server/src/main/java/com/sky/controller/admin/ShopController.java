package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")  // 这里给控制器添加一个别名，防止对应的Bean对象在IOC容器中冲突
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags="店铺相关接口")
public class ShopController {
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态：{}",status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set("SHOP_STATUS", status); // 使用Redis保存店铺营业状态
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping
    @ApiOperation("管理端-获取店铺营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
        log.info("管理端-获取店铺营业状态:{}",  status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
