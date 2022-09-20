package cn.wolfcode.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.feign.ProductFeignApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductFeignFallback implements ProductFeignApi {
    @Override
    public Result<List<Product>> findProductByIds(List<Long> productIds) {

        return null;
    }
}
