package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.fallback.SeckillProductFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "seckill-service", fallback = SeckillProductFeignFallback.class)
public interface SeckillProductFeignApi {

    @RequestMapping("/seckillProduct/findAllSeckillProduct")
    Result<List<SeckillProductVo>> findAllSeckillProduct();
}
