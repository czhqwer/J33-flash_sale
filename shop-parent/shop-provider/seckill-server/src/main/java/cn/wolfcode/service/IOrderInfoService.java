package cn.wolfcode.service;


import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.mq.OrderMessage;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;


/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    String order(OrderMessage message);

    OrderInfo findByOrderNo(String orderNo);

    int changeOrderStatusToTimeout(String orderNo);

    String alipay(String orderNo, Integer type);

    void returnUrl(Map<String, String> params, HttpServletResponse response);

    String notifyUrl(Map<String, String> params);

}
