package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController") // 这里给控制器添加一个别名，防止对应的Bean对象在IOC容器中冲突
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags="店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("用户端-获取店铺营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
        log.info("用户端-获取店铺营业状态:{}",  status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
