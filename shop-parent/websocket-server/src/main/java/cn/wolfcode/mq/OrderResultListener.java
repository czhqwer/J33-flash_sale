package cn.wolfcode.mq;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = "order_result_topic",
        topic = MQConstant.ORDER_RESULT_TOPIC
)
@Component
public class OrderResultListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(OrderMQResult result) {

    }
}