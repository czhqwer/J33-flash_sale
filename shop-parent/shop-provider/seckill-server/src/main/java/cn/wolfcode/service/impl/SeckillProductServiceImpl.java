package cn.wolfcode.service.impl;

import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.service.ISeckillProductService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired(required = false)
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public List<SeckillProductVo> findAllSeckillProduct() {

        return null;
    }
}
