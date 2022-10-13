package cn.wolfcode.service.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.feign.IntergralFeignApi;
import cn.wolfcode.feign.PayFeignApi;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private PayLogMapper payLogMapper;
    @Resource
    private RefundLogMapper refundLogMapper;

    @Resource
    private PayFeignApi payFeignApi;
    @Resource
    private IntergralFeignApi intergralFeignApi;

    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;

    @Override
    @Transactional
    public String order(OrderMessage message) {
        //判断是否重复下单
        OrderInfo orderInfo = orderInfoMapper.findOrderByUidAndSid(message.getUserPhone(), message.getSeckillId());
        if (!StringUtils.isEmpty(orderInfo)) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //判断是否存在库存
        SeckillProductVo seckillProductVo = seckillProductService.find(message.getTime(), message.getSeckillId());
        if (seckillProductVo.getStockCount() <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //插入一条order信息，到表中，状态为未支付
        OrderInfo order = new OrderInfo();
        //雪花算法生成的id，通常用于分布式项目的id生成，id是有顺序的
        String orderNo = String.valueOf(IdGenerateUtil.get().nextId());

        order.setOrderNo(orderNo);

        order.setUserId(message.getUserPhone());
        order.setProductId(seckillProductVo.getProductId());
        order.setDeliveryAddrId(null);
        order.setProductName(seckillProductVo.getProductName());
        order.setProductImg(seckillProductVo.getProductImg());
        order.setProductCount(1);
        order.setProductPrice(seckillProductVo.getProductPrice());
        order.setSeckillPrice(seckillProductVo.getSeckillPrice());
        order.setIntergral(seckillProductVo.getIntergral());
        order.setStatus(OrderInfo.STATUS_ARREARAGE); //未支付
        order.setCreateDate(new Date());
        order.setPayDate(null);
        order.setPayType(OrderInfo.PAYTYPE_ONLINE);
        order.setSeckillDate(new Date());
        order.setSeckillTime(message.getTime());
        order.setSeckillId(message.getSeckillId());

        int m = orderInfoMapper.insert(order);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_ERROR);
        }
        //真实库存减一
        m = seckillProductService.descStoreCount(message.getSeckillId());
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_ERROR);
        }
        //在Redis中添加订单信息(这段代码已经移动到cancel中操作)
        /**
         * 正常应该注释掉，为在OrderInfoHandler已经有了，但是cancel没有生效，所以先放开
         */
        //TODO cancel bug：没有生效
        redisTemplate.opsForSet().add(SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(message.getUserPhone())),
                String.valueOf(message.getSeckillId()));
        return orderNo; //订单编号
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    @Override
    public int changeOrderStatusToTimeout(String orderNo) {
        return orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
    }
    @Override
    public String alipay(String orderNo, Integer type) {
        String ret = null;
        switch (type) {
            case OrderInfo.PAYTYPE_ONLINE: //在线支付
                ret = payOnLine(orderNo);
                break;
            case OrderInfo.PAYTYPE_INTERGRAL: //积分支付
                ret = payOnIntegral(orderNo);
                break;
        }
        return ret;
    }

    @Override
    public void returnUrl(Map<String, String> params, HttpServletResponse response) {
        //验签操作
        Result<Boolean> result = payFeignApi.rsaCheck(params);
        if (StringUtils.isEmpty(result) || result.hasError()) {
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        if (result.getData()) {
            //判断notify方法是否已经执行完成 如果没有执行，重定向到一个错误页面
            OrderInfo orderInfo = orderInfoMapper.find(params.get("out_trade_no"));
            if (orderInfo != null && orderInfo.getStatus() == OrderInfo.STATUS_ARREARAGE) {
                throw new RuntimeException("系统忙，请稍后再查询");
            }
            //重定向到一个页面
            //frontEndPayUrl: http://localhost/order_detail.html?orderNo=
            try {
                response.sendRedirect(frontEndPayUrl + params.get("out_trade_no"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public String notifyUrl(Map<String, String> params) {
        //验签操作
        Result<Boolean> result = payFeignApi.rsaCheck(params);
        if (StringUtils.isEmpty(result) || result.hasError()) {
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        String ret = "success";
        if (result.getData()) {
            //处理业务逻辑
            try {
                PayLog payLog = new PayLog();
                payLog.setTradeNo(params.get("trade_no"));
                payLog.setOutTradeNo(params.get("out_trade_no"));
                payLog.setNotifyTime(new Date().getTime() + "");
                payLog.setTotalAmount(params.get("total_amount"));
                payLog.setPayType(PayLog.PAY_TYPE_ONLINE);
                payLogMapper.insert(payLog);//添加支付日志
                System.out.println(params.get("out_trade_no"));

                //修改订单状态为 已付款
                orderInfoMapper.changePayStatus(
                        params.get("out_trade_no"),
                        OrderInfo.STATUS_ACCOUNT_PAID,
                        OrderInfo.PAYTYPE_ONLINE);
            } catch (Exception e) {
                e.printStackTrace();
                //处理失败 ret = fail
                ret = "fail";
            }
        } else {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
        return ret;
    }

    @Override
    @Transactional
    public String refund(String orderNo) {
        String ret = "退款成功";
        //通过订单编号查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        if (StringUtils.isEmpty(orderInfo)) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        //根据订单的支付类型，走支付宝退款或者积分退款
        switch (orderInfo.getPayType()) {
            case OrderInfo.PAYTYPE_ONLINE:
                ret = refundOnLine(orderInfo);
                break;
            case OrderInfo.PAYTYPE_INTERGRAL:
                ret = refundOnIntegral(orderInfo);
                break;
        }
        return ret;
    }

    private String refundOnIntegral(OrderInfo orderInfo) {
        //远程调用积分服务，进行退积分
        OperateIntergralVo vo = new OperateIntergralVo();
        vo.setPk(orderInfo.getOrderNo());
        vo.setValue(orderInfo.getIntergral());
        vo.setInfo("今天气温12");
        vo.setUserId(orderInfo.getUserId());

        Result<Boolean> result = intergralFeignApi.refund(vo);
        if (StringUtils.isEmpty(result) || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        //如果成功，修改订单状态为已退款
        int m = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_ERROR);
        }
        //记录退款日志
        RefundLog refundLog = new RefundLog();
        refundLog.setOutTradeNo(orderInfo.getOrderNo());
        refundLog.setRefundAmount(orderInfo.getIntergral().toString());
        refundLog.setRefundReason(vo.getInfo());
        refundLog.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
        refundLog.setRefundTime(new Date());
        m = refundLogMapper.insert(refundLog);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_ERROR);
        }
        return "积分退款成功";
    }

    private String payOnIntegral(String orderNo) {
        //通过订单编号，查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        if (StringUtils.isEmpty(orderInfo)) {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
        //远程调用积分服务 执行积分扣款
        OperateIntergralVo vo = new OperateIntergralVo();
        vo.setPk(orderNo);
        vo.setValue(orderInfo.getIntergral());
        vo.setInfo("积分太多了，用不完");
        vo.setUserId(orderInfo.getUserId());
        System.out.println(vo);

        Result<Boolean> result = intergralFeignApi.pay(vo);
        if (StringUtils.isEmpty(result) || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
        //修改订单状态为已付款
        int m = orderInfoMapper.changePayStatus(orderNo,
                OrderInfo.STATUS_ACCOUNT_PAID,
                OrderInfo.PAYTYPE_INTERGRAL);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }

        //填写付款日志
        PayLog payLog = new PayLog();
        payLog.setTradeNo(orderNo);
        payLog.setOutTradeNo(orderNo);
        payLog.setNotifyTime(new Date().getTime() + "");
        payLog.setTotalAmount(orderInfo.getIntergral().toString());
        payLog.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
        m = payLogMapper.insert(payLog);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }

        return "积分支付成功";
    }

    private String refundOnLine(OrderInfo orderInfo) {
        //远程调用支付服务 退款
        RefundVo refundVo = new RefundVo();
        refundVo.setOutTradeNo(orderInfo.getOrderNo());
        refundVo.setRefundAmount(orderInfo.getSeckillPrice().toString());
        refundVo.setRefundReason("不想要了");

        Result<Boolean> result = payFeignApi.refund(refundVo);
        if (StringUtils.isEmpty(result) || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        //如果退款失败，返回的字符串 ”退款失败，请联系客服“
        if (!result.getData()) {
            return "退款失败，请联系客服人员";
        }
        //如果退款成功，修改订单状态为已退款
        int m = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        //记录退款日志
        RefundLog refundLog = new RefundLog();
        refundLog.setOutTradeNo(orderInfo.getOrderNo());
        refundLog.setRefundAmount(orderInfo.getSeckillPrice().toString());
        refundLog.setRefundReason(refundVo.getRefundReason());
        refundLog.setRefundType(OrderInfo.PAYTYPE_ONLINE);
        refundLog.setRefundTime(new Date());
        m = refundLogMapper.insert(refundLog);
        if (m <= 0) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        return "退款成功！";
    }

    //实现支付宝支付功能
    private String payOnLine(String orderNo) {
        //通过订单编号 查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //远程掉i用支付服务 进行付款
        PayVo payVo = new PayVo();
        payVo.setOutTradeNo(orderNo);
        payVo.setTotalAmount(orderInfo.getSeckillPrice().toString());
        payVo.setSubject(orderInfo.getProductName());
        payVo.setBody("这个太赚了！");
        payVo.setReturnUrl(returnUrl); //同步访问地址
        payVo.setNotifyUrl(notifyUrl); //异步访问地址
        Result<String> result = payFeignApi.pay(payVo);
        if (StringUtils.isEmpty(result) || result.hasError()) {
            throw new BusinessException(CommonCodeMsg.RESULT_INVALID);
        }
        return result.getData();
    }

}
