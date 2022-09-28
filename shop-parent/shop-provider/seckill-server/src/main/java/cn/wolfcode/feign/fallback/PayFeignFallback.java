package cn.wolfcode.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.Product;
import cn.wolfcode.feign.PayFeignApi;
import cn.wolfcode.feign.ProductFeignApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayFeignFallback implements PayFeignApi {

    @Override
    public Result<String> pay(PayVo payVo) {
        return null;
    }
}
