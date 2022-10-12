package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.feign.fallback.IntergralFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "intergral-service", fallback = IntergralFeignFallback.class)
public interface IntergralFeignApi {

    @RequestMapping("/intergral/pay")
    Result<Boolean> pay(@RequestBody OperateIntergralVo vo);

    @RequestMapping("/intergral/refund")
    Result<Boolean> refund(@RequestBody OperateIntergralVo vo);

}
