package cn.wolfcode.redis;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * Created by wolfcode-lanxw
 */
@Getter
public enum SeckillRedisKey {
    SECKILL_PRODUCT_HASH("seckillProductHash:"),//秒杀商品的前缀
    SECKILL_ORDER_SET("seckillOrderSet:"), //秒杀商品的信息前缀
    SECKILL_STOCK_COUNT_HASH("seckillStockCount:"),//库存的前缀
    SECKILL_REAL_COUNT_HASH("seckillRealCount:");
    SeckillRedisKey(String prefix, TimeUnit unit, int expireTime){
        this.prefix = prefix;
        this.unit = unit;
        this.expireTime = expireTime;
    }
    SeckillRedisKey(String prefix){
        this.prefix = prefix;
    }
    public String getRealKey(String key){
        return this.prefix+key;
    }
    private String prefix;
    private TimeUnit unit;
    private int expireTime;
}
