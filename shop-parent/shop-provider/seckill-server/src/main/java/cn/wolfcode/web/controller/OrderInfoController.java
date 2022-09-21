package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.CommonRedisKey;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    public static ConcurrentHashMap<Long, Boolean> LOCAL_FLAG = new ConcurrentHashMap<>();

    @RequireLogin //必须是登录用户才能访问这个方法
    @PostMapping("/doSeckill")
    public Result<String> doSeckill(Long seckillId, Integer time, HttpServletRequest request) {
        // 1 判断本地标识，如果为true，放行，false，表示秒杀结束
        if (LOCAL_FLAG.get(seckillId) == null) {
            LOCAL_FLAG.put(seckillId, true);
        } else if (LOCAL_FLAG.get(seckillId) == false) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }

        // 2 判断是否是重复下单，从Redis中查看是否存在这个订单信息, userId+seckillId
        //key 前缀=userId(手机号), seckillId(秒杀商品id)
        //获取Token
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        //根据token获取userInfo
        String userInfoJson = redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token));
        UserInfo userInfo = JSON.parseObject(userInfoJson, UserInfo.class);
        Boolean isMember = redisTemplate.opsForSet()
                .isMember(SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(userInfo.getPhone().toString()),
                        String.valueOf(seckillId));
        if (isMember) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_SECKILL);
        }
        // 3 判断库存是否小于等于0 秒杀结束了
        String bigKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
        String smallKey = String.valueOf(seckillId);
//        String storeCountStr = (String) redisTemplate.opsForHash().get(bigKey, smallKey);
//        if (Integer.parseInt(storeCountStr) <= 0) {
//            LOCAL_FLAG.put(seckillId, false);
//            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
//        }

        // 4 上面都不是，redis预库存-1操作，判断-1后的值必须具有原子性
        // 返回值如果<=0，设置本地标识为false
        Long redisStore = redisTemplate.opsForHash().increment(bigKey, smallKey, -1);
        if (redisStore <= 0) {
            LOCAL_FLAG.put(seckillId, false);
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }

        // 5 发送mq消息，判断返回信息，如果成功"秒杀商品完成，请等待结果！"
        //    “您秒杀的上商品已经卖完了，欢迎下次抢购”， 还要做Redis库存+1，本地标识设为true
        OrderMessage message = new OrderMessage();
        message.setTime(time);
        message.setSeckillId(seckillId);
        message.setToken(token);
        message.setUserPhone(userInfo.getPhone());

        SendResult sendResult = rocketMQTemplate.syncSend(MQConstant.ORDER_PEDDING_TOPIC, message);
        String ret = "秒杀商品完成，请等待结果！";
        if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
            ret =  "您秒杀的上商品已经卖完了，欢迎下次抢购";
            //同步预库存 从数据库中去出库存放到Redis中
            SeckillProduct seckillProduct = seckillProductService.findSeckillProductBySeckillId(seckillId);
            redisTemplate.opsForHash().put(bigKey, smallKey, String.valueOf(seckillProduct.getStockCount()));
            System.err.println("发消息");

            //设置本地标识为true 广播
            rocketMQTemplate.syncSend(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC, seckillId);
        }

        return Result.success(ret);
    }
}
