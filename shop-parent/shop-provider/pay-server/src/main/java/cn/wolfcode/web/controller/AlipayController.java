package cn.wolfcode.web.controller;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.config.AlipayConfig;
import cn.wolfcode.config.AlipayProperties;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/alipay")
public class AlipayController {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayProperties alipayProperties;


    /**
     * @param payVo
     * @return
     */
    @RequestMapping("/pay")
    Result<String> pay(@RequestBody PayVo payVo) {
        if (StringUtils.isEmpty(payVo)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(payVo.getReturnUrl());
        alipayRequest.setNotifyUrl(payVo.getNotifyUrl());

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = payVo.getOutTradeNo();
        //付款金额，必填
        String total_amount = payVo.getTotalAmount();
        //订单名称，必填
        String subject = payVo.getSubject();
        //商品描述，可空
        String body = payVo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //请求
        String result = null; //一个表单+一个js提交
        try {
            result = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return Result.success(result);
    }

    /**
     * @param params
     * @return
     */
    @RequestMapping("/rsaCheck")
    Result<Boolean> rsaCheck(@RequestParam Map<String, String> params) {
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType());
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return Result.success(signVerified);
    }

    @RequestMapping("/refund")
    Result<Boolean> refund(@RequestBody RefundVo refundVo) {
        if (StringUtils.isEmpty(refundVo)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        //设置请求参数
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();

        //商户订单号，商户网站订单系统中唯一订单号
        String out_trade_no = refundVo.getOutTradeNo();
        //支付宝交易号
//        String trade_no = refundVo.getOutTradeNo();
        //请二选一设置
        //需要退款的金额，该金额不能大于订单金额，必填
        String refund_amount = refundVo.getRefundAmount();
        //退款的原因说明
        String refund_reason = refundVo.getRefundReason();
        //标识一次退款请求，同一笔交易多次退款需要保证唯一，如需部分退款，则此参数必传
//        String out_request_no = new String(request.getParameter("WIDTRout_request_no").getBytes("ISO-8859-1"),"UTF-8");

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
//                + "\"trade_no\":\""+ trade_no +"\","
                + "\"refund_amount\":\""+ refund_amount +"\","
                + "\"refund_reason\":\""+ refund_reason +"\"}");
//                + "\"out_request_no\":\""+ out_request_no +"\"" +

        Boolean ret = false;
        //请求
        try {
            AlipayTradeRefundResponse execute = alipayClient.execute(alipayRequest);
            ret = execute.isSuccess();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return Result.success(ret);
    }
}
