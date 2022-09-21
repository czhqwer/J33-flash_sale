package cn.wolfcode.service;


import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.mq.OrderMessage;


/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    String order(OrderMessage message);

    OrderInfo findByOrderNo(String orderNo);

    int changeOrderStatusToTimeout(String orderNo);

}
