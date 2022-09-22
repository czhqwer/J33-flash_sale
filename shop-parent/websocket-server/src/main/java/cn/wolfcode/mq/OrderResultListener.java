package cn.wolfcode.mq;


import cn.wolfcode.server.WebSocketServer;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        //接受MQ消息，取出Token
        String token = result.getToken();
        //获取map中的WebSocketServer对象
        WebSocketServer server = null;
        //如果这个对象不存在，每秒钟获取一次，一共获取三次
        for (int i = 0; i < 3 && (server = WebSocketServer.clients.get(token)) == null; i++) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //server如果还没有，啥都不处理，等待超时取消
        if (server != null) {
            try {
                //getSession具体websocket实例 BasicRemote连接这个实例的远程对象 sendText发送消息
                server.getSession().getBasicRemote().sendText(JSON.toJSONString(result));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //关闭长连接
                try {
                    server.getSession().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}