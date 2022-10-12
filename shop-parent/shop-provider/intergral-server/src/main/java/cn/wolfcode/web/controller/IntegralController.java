package cn.wolfcode.web.controller;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.service.IUsableIntegralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/intergral")
public class IntegralController {
    @Autowired
    private IUsableIntegralService usableIntegralService;

    @RequestMapping("/pay")
    Result<Boolean> pay(@RequestBody OperateIntergralVo vo) {
        if (StringUtils.isEmpty(vo)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        return Result.success(usableIntegralService.pay(vo));
    }

    @RequestMapping("/refund")
    Result<Boolean> refund(@RequestBody OperateIntergralVo vo) {
        if (StringUtils.isEmpty(vo)) {
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        }
        return Result.success(usableIntegralService.refund(vo));
    }

}
