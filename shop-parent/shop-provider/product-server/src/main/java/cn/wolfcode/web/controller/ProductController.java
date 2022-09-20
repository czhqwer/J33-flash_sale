package cn.wolfcode.web.controller;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/product")
@Slf4j
public class ProductController {
    @Autowired
    private IProductService productService;

    @RequestMapping("/findProductByIds")
    public Result<List<Product>> findProductByIds(@RequestParam List<Long> productIds) {
        // 1. 参数认证
        if(StringUtils.isEmpty(productIds)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        // 2. 调用服务
        return Result.success(productService.findProductByIds(productIds));
    }
}
