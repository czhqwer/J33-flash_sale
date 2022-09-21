package cn.wolfcode.mq.listener;


import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RocketMQMessageListener(
        consumerGroup = "order_pay_timeout_topic",
        topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC
)
@Component
public class OrderPayTimeoutListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Transactional
    @Override
    public void onMessage(OrderMQResult result) {
        //10s以后
        //判断订单状态 是否是未支付
        OrderInfo orderInfo = orderInfoService.findByOrderNo(result.getOrderNo());
        if (StringUtils.isEmpty(orderInfo) ||
                !orderInfo.getStatus().equals(OrderInfo.STATUS_ARREARAGE)) {
            return;
        }
        //设置订单状态为超时取消
        int m = orderInfoService.changeOrderStatusToTimeout(result.getOrderNo());
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.CANCEL_ORDER_ERROR);
        }
        //修改真是库存+1
        m = seckillProductService.incrStoreCount(result.getSeckillId());
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.CANCEL_ORDER_ERROR);
        }
        //同步Redis预库存
        SeckillProduct seckillProduct = seckillProductService.findSeckillProductBySeckillId(result.getSeckillId());

        String bigKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(result.getTime()));
        String smallKey = String.valueOf(result.getSeckillId());
        redisTemplate.opsForHash().put(bigKey, smallKey, String.valueOf(seckillProduct.getStockCount()));
        System.err.println("发消息");

        //广播消息修改本地标识为true
        rocketMQTemplate.syncSend(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC, result.getSeckillId());

    }
}
