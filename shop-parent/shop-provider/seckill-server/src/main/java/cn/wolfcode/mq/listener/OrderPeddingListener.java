package cn.wolfcode.mq.listener;


import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMQResult;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = "order_pedding_listener",
        topic = MQConstant.ORDER_PEDDING_TOPIC
)
@Component
public class OrderPeddingListener implements RocketMQListener<OrderMessage> {

    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(OrderMessage message) {
        OrderMQResult result = new OrderMQResult(); //继续发MQ，用这个消息传递
        result.setTime(message.getTime());
        result.setSeckillId(message.getSeckillId());
        result.setToken(message.getToken());
        try {
            //调用 service层 进行表级下单操作
            String orderNo = orderInfoService.order(message);
            result.setOrderNo(orderNo);
            //如果成功，发送延时消息
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,
                    MessageBuilder.withPayload(result).build(),
                    5000,
                    MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL); //10s
        } catch (Exception e) {
            e.printStackTrace();
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            //如果失败，发送失败消息
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC + ":" +
                    MQConstant.ORDER_RESULT_FAIL_TAG, result);
        } finally {
            //无论成功失败，发送结果消息
//            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC, result);
        }
        System.out.println(result);
    }
}
