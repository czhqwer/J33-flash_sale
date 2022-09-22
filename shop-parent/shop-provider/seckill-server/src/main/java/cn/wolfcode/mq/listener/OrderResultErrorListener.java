package cn.wolfcode.mq.listener;


import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = "order_result_error_topic",
        topic = MQConstant.ORDER_RESULT_TOPIC,
        selectorType = SelectorType.TAG,
        selectorExpression = MQConstant.ORDER_RESULT_FAIL_TAG
)
@Component
public class OrderResultErrorListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ISeckillProductService seckillProductService;

    @Override
    public void onMessage(OrderMQResult result) {
        //同步Redis预库存
        SeckillProduct seckillProduct = seckillProductService.findSeckillProductBySeckillId(result.getSeckillId());

        String bigKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(result.getTime()));
        String smallKey = String.valueOf(result.getSeckillId());
        redisTemplate.opsForHash().put(bigKey, smallKey, String.valueOf(seckillProduct.getStockCount()));
        System.err.println("OrderResultErrorListener发消息");

        //广播消息修改本地标识为true
        rocketMQTemplate.syncSend(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC, result.getSeckillId());

    }
}