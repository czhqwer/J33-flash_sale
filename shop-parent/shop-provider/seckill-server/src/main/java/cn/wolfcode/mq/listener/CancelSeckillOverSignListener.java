package cn.wolfcode.mq.listener;

import cn.wolfcode.web.controller.OrderInfoController;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = "cancelseckilloversignlistener",
        topic = MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC,
        messageModel = MessageModel.BROADCASTING
)
@Component
public class CancelSeckillOverSignListener implements RocketMQListener<Long> {
    @Override
    public void onMessage(Long seckillId) {
        System.out.println("广播消息：" + seckillId + "重置本地标识为true");
        OrderInfoController.LOCAL_FLAG.put(seckillId, true);
    }
}
