package cn.wolfcode.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import cn.wolfcode.feign.IntergralFeignApi;
import cn.wolfcode.feign.PayFeignApi;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IntergralFeignFallback implements IntergralFeignApi {

    @Override
    public Result<Boolean> pay(OperateIntergralVo vo) {
        return null;
    }

    @Override
    public Result<Boolean> refund(OperateIntergralVo vo) {
        return null;
    }
}
