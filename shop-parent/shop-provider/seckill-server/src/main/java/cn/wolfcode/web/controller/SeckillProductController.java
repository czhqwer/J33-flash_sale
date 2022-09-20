package cn.wolfcode.web.controller;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/seckillProduct")
@Slf4j
public class SeckillProductController {
    @Autowired
    private ISeckillProductService seckillProductService;

    @RequestMapping("/findAllSeckillProduct")
    public Result<List<SeckillProductVo>> findAllSeckillProduct(){
        //1 验证参数
        //2 调用服务
        List<SeckillProductVo> seckillProductList = seckillProductService.findAllSeckillProduct();
        return Result.success(seckillProductList);
    }

    @RequestMapping("/queryByTime")
    public Result<List<SeckillProductVo>> queryByTime(Integer time) {
        //验证参数
        if(StringUtils.isEmpty(time))
            throw  new BusinessException(CommonCodeMsg.PARAM_INVALID);
        //调用服务
        return Result.success(seckillProductService.queryByTime(time));
    }


    @RequestMapping("/find")
    public Result<SeckillProductVo> find(Integer time, Long seckillId) {
        if(StringUtils.isEmpty(time) || StringUtils.isEmpty(seckillId))
            throw new BusinessException(CommonCodeMsg.PARAM_INVALID);
        return Result.success(seckillProductService.find(time, seckillId));
    }
}
