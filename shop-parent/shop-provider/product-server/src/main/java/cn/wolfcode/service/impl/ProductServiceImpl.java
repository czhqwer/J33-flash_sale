package cn.wolfcode.service.impl;

import cn.wolfcode.domain.Product;
import cn.wolfcode.mapper.ProductMapper;
import cn.wolfcode.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


@Service
public class ProductServiceImpl implements IProductService {
    @Autowired(required = false)
    private ProductMapper productMapper;

    @Override
    public List<Product> findProductByIds(List<Long> productIds) {
        if(StringUtils.isEmpty(productIds) || productIds.size() == 0) {
            return new ArrayList<>();
        }
        return productMapper.queryProductByIds(productIds);
        }
}
