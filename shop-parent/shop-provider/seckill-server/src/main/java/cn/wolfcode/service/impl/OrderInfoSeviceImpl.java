package cn.wolfcode.service.impl;

import java.util.Date;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.SeckillProductVo;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

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
//        redisTemplate.opsForSet().add(SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(message.getUserPhone())),
//                String.valueOf(message.getSeckillId()));
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
        switch(type) {
            case OrderInfo.PAYTYPE_ONLINE: //在线支付
                ret = payOnLine(orderNo);
                break;
            case OrderInfo.PAYTYPE_INTERGRAL: //积分支付
                break;
        }
        return ret;
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
