package cn.wolfcode.web.controller;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.service.IOrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @RequestMapping("/alipay")
    @RequireLogin
    public Result<String> alipay(String orderNo, Integer type) {
        if (StringUtils.isEmpty(orderNo) || StringUtils.isEmpty(type)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        return Result.success(orderInfoService.alipay(orderNo, type));
    }

    @RequestMapping("/return_url")
    public void returnUrl(@RequestParam Map<String, String> params, HttpServletResponse response) {
        if (StringUtils.isEmpty(params)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        orderInfoService.returnUrl(params, response);
    }

    @RequestMapping("/notify_url")
    public String notifyUrl(@RequestParam Map<String, String> params) {
        if (StringUtils.isEmpty(params)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        //success fail
        return orderInfoService.notifyUrl(params);
    }

    @RequestMapping("/refund")
    @RequireLogin
    public Result<String> refund(String orderNo) {
        if (StringUtils.isEmpty(orderNo)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        return  Result.success(orderInfoService.refund(orderNo));
    }
}
