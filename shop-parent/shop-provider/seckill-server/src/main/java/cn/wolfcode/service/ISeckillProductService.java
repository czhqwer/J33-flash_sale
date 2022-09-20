package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;


public interface ISeckillProductService {
    List<SeckillProductVo> findAllSeckillProduct();

    List<SeckillProductVo> queryByTime(Integer time);

    SeckillProductVo find(Integer time, Long seckillId);

    SeckillProduct findSeckillProductBySeckillId(Long seckillId);

    int descStoreCount(Long seckillId);

}
