package cn.wolfcode.job;

import cn.wolfcode.redis.JobRedisKey;
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

import java.util.Calendar;
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
public class UserCacheJob implements SimpleJob {
    @Value("${jobCron.userCache}")
    private String cron;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public void execute(ShardingContext shardingContext) {
        doWork();
    }
    private void doWork() { //每天0:10
        Calendar calendar = Calendar.getInstance(); //获取当前时间
        calendar.add(Calendar.DATE,-7); //调整时间为七天前
        //获取7天前的日期
        Long max = calendar.getTime().getTime(); //获得七天前的时间戳
        String userZSetKey = JobRedisKey.USER_ZSET.getRealKey("");
        String userHashKey = JobRedisKey.USER_HASH.getRealKey("");
        //获取所有zset的key值
        Set<String> ids = redisTemplate
                .opsForZSet()
                .rangeByScore(userZSetKey, 0, max); //获取分数范围内的所有key值 0-7天前
        //删除7天前的用户缓存数据
        if(ids.size()>0){
            //big key    samll key[]
            //删除7天前所有的hset
            redisTemplate.opsForHash().delete(userHashKey,ids.toArray());
        }
        //redisTemplate.delete(ids); //这样也能删，但是多线程条件下不建议
        //删除7天前所有的zset
        redisTemplate.opsForZSet()
                .removeRangeByScore(
                        userZSetKey,
                        0,
                        calendar.getTime().getTime());
    }
}