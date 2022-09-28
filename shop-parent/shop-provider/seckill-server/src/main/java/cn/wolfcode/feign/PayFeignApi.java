package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.feign.fallback.PayFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "pay-service", fallback = PayFeignFallback.class)
public interface PayFeignApi {

    @RequestMapping("/alipay/pay")
    Result<String> pay(@RequestBody PayVo payVo);

    @RequestMapping("/alipay/rsaCheck")
    Result<Boolean> rsaCheck(@RequestParam Map<String, String> params);

}
