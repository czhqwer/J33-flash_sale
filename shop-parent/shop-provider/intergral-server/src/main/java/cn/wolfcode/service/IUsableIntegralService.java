package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntergralVo;


public interface IUsableIntegralService {

    Boolean pay(OperateIntergralVo vo);

    Boolean refund(OperateIntergralVo vo);

}
