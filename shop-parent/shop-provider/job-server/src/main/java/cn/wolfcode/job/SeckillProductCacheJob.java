package cn.wolfcode.job;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.SeckillProductFeignApi;
import cn.wolfcode.redis.JobRedisKey;
import cn.wolfcode.redis.SeckillRedisKey;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**

 * 用于处理用户缓存的定时任务
 * 为了保证Redis中的内存的有效使用。
 * 我们默认保留7天内的用户缓存数据，每天凌晨的时候会把7天前的用户登录缓存数据删除掉
 */
@Component
@Setter@Getter
@RefreshScope
@Slf4j
public class SeckillProductCacheJob implements SimpleJob {
    @Value("${jobCron.initSeckillProduct}")
    private String cron;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource //jdk提供的自动装配注解，默认是byName, Autowire byType
    private SeckillProductFeignApi seckillProductFeignApi;



    @Override
    public void execute(ShardingContext shardingContext) {
        doWork();
    }
    private void doWork() { //每天0:10
        //1 删除redis中历史数据
        int[] times = {10, 12, 14};
        // key在秒杀服务，job服务中用 SeckillRedisKey.SECKILL_PRODUCT_HASH:time
        List<String> SeckillProductkeys = new ArrayList<>();
        List<String> StoreCountkeys = new ArrayList<>();
        for (int time : times) {
            String SeckillProductBigKey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
            String StoreCountBigKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            SeckillProductkeys.add(SeckillProductBigKey);
            StoreCountkeys.add(StoreCountBigKey);
        }
        redisTemplate.delete(SeckillProductkeys);
        redisTemplate.delete(StoreCountkeys);
        //2 远程调用Seckill-server 获取今天的秒杀商品列表
        Result<List<SeckillProductVo>> result = seckillProductFeignApi.findAllSeckillProduct();
        if(StringUtils.isEmpty(result) //降级
                || result.hasError()) { //异常引起的 !=200
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        //3 把信息存储到redis中（商品信息，库存信息）
        for (SeckillProductVo vo : result.getData()) {
            String bigkey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(vo.getTime()));
            String smallkey = String.valueOf(vo.getId());
            redisTemplate.opsForHash().put(bigkey, smallkey, JSON.toJSONString(vo));
        }
        for (SeckillProductVo vo : result.getData()) {
            String bigkey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(vo.getTime()));
            String smallkey = String.valueOf(vo.getId());
            redisTemplate.opsForHash().put(bigkey, smallkey, String.valueOf(vo.getStockCount()));
        }
    }
}