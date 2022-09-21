package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.ProductFeignApi;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.*;


@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired(required = false)
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ProductFeignApi productFeignApi;

    private List<SeckillProductVo> findSeckillProductUtil(List<SeckillProduct> seckillProductList) {
        List<Long> productIds = new ArrayList<>();
        // 2. SeckillProduct有productId 依据id远程调用产品服务 查询List<Product>
        for (SeckillProduct seckillProduct : seckillProductList) {
            Long productId = seckillProduct.getProductId();
            productIds.add(productId);
        }
        // 3. 远程调用product-server
        Result<List<Product>> result = productFeignApi.findProductByIds(productIds);
        if (StringUtils.isEmpty(result) //降级 =null
                || result.hasError()) { //异步引起的 != 200
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        // 4. list数据转换为map数据
        Map<Long, Product> productMap = new HashMap<>();
        for (Product prod : result.getData()) {
            productMap.put(prod.getId(), prod);
        }
        // 5. 生成List<SeckillProductVo>返回值
        List<SeckillProductVo> ret = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProductList) {
            Product product = productMap.get(seckillProduct.getProductId());
            SeckillProductVo vo = new SeckillProductVo();
            BeanUtils.copyProperties(product, vo); //id = 产品id
            BeanUtils.copyProperties(seckillProduct, vo); //id是秒杀id
            ret.add(vo);
        }
        return ret;
    }

    @Override
    public List<SeckillProductVo> findAllSeckillProduct() {
        // 1. 查询秒杀商品表的数据 List<SeckillProduct> 查询当天所有秒杀商品
        List<SeckillProduct> seckillProductList = seckillProductMapper.queryAllSeckillProduct();

        /*List<Long> productIds = new ArrayList<>();
        // 2. SeckillProduct有productId 依据id远程调用产品服务 查询List<Product>
        for (SeckillProduct seckillProduct : seckillProductList) {
            Long productId = seckillProduct.getProductId();
            productIds.add(productId);
        }
        // 3. 远程调用product-server
        Result<List<Product>> result = productFeignApi.findProductByIds(productIds);
        if (StringUtils.isEmpty(result) //降级 =null
                || result.hasError()) { //异步引起的 != 200
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        // 4. list数据转换为map数据
        Map<Long, Product> productMap = new HashMap<>();
        for (Product prod : result.getData()) {
            productMap.put(prod.getId(), prod);
        }
        // 5. 生成List<SeckillProductVo>返回值
        List<SeckillProductVo> ret = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProductList) {
            Product product = productMap.get(seckillProduct.getProductId());
            SeckillProductVo vo = new SeckillProductVo();
            BeanUtils.copyProperties(product, vo); //id = 产品id
            BeanUtils.copyProperties(seckillProduct, vo); //id是秒杀id
            ret.add(vo);
        }*/
        List<SeckillProductVo> ret = findSeckillProductUtil(seckillProductList);
        return ret;
    }

    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
        //从Redis中获取对应的代码，如果没有，从数据库中获取
        String bigkey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Collection<Object> values = redisTemplate.opsForHash().entries(bigkey).values();
        List<SeckillProductVo> ret = new ArrayList<>();
        for (Object json : values) {
            SeckillProductVo temp = JSON.parseObject((String)json, SeckillProductVo.class);
            ret.add(temp);
        }
        if(ret.size() > 0) {
            return ret;
        }

        //如果Redis没有
        //先查询秒杀库的集合
        List<SeckillProduct> seckillProductList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        //远程调用商品微服务，获取对应数据
        //合并代码到VO
        ret = findSeckillProductUtil(seckillProductList);
        //放入到Redis
        for (SeckillProductVo vo : ret) {
            bigkey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(vo.getTime()));
            String smallkey = String.valueOf(vo.getId());
            redisTemplate.opsForHash().put(bigkey, smallkey, JSON.toJSONString(vo));
        }
        for (SeckillProductVo vo : ret) {
            bigkey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(vo.getTime()));
            String smallkey = String.valueOf(vo.getId());
            redisTemplate.opsForHash().put(bigkey, smallkey, String.valueOf(vo.getStockCount()));
        }
        return ret;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        //从Redis中获取VO对象
        String bigkey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        String json = (String) redisTemplate.opsForHash().get(bigkey, String.valueOf(seckillId));
        SeckillProductVo ret = JSON.parseObject(json, SeckillProductVo.class);
        if(!StringUtils.isEmpty(ret))
            return ret;
        //如果Redis中没有，从Mysql中获取
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        //准备参数数据
        List<SeckillProduct> seckillProductList = Arrays.asList(seckillProduct);
        //调用工具类，返回vo集合
        List<SeckillProductVo> seckillProductVoList = findSeckillProductUtil(seckillProductList);
        if(!StringUtils.isEmpty(seckillProductVoList) && seckillProductVoList.size()==1)
            ret = seckillProductVoList.get(0);
        //如果MySQL中获取到，放入到Redis中
        if (!StringUtils.isEmpty(ret)) {
            bigkey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
            String smallkey = String.valueOf(seckillId);
            redisTemplate.opsForHash().put(bigkey, smallkey, JSON.toJSONString(ret));
            bigkey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            redisTemplate.opsForHash().put(bigkey, smallkey, String.valueOf(ret.getStockCount()));
        }
        return ret;
    }

    @Override
    public SeckillProduct findSeckillProductBySeckillId(Long seckillId) {
        return seckillProductMapper.find(seckillId);
    }

    @Override
    public int descStoreCount(Long seckillId) {
        return seckillProductMapper.decrStock(seckillId);
    }

    @Override
    public int incrStoreCount(Long seckillId) {
        return seckillProductMapper.incrStock(seckillId);
    }

}
