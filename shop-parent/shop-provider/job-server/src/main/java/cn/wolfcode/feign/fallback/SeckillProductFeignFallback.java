package cn.wolfcode.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.SeckillProductFeignApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillProductFeignFallback implements SeckillProductFeignApi {
    @Override
    public Result<List<SeckillProductVo>> findAllSeckillProduct() {
        return null;//当返回值是null的时候，表示降级了
    }
}
