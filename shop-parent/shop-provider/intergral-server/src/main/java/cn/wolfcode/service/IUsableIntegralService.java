package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntergralVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface IUsableIntegralService {

    Boolean pay(OperateIntergralVo vo);

    //    Boolean refund(OperateIntergralVo vo); //一个变成三个

    @TwoPhaseBusinessAction(
            name = "refundTry",
            commitMethod = "refundConfirm",
            rollbackMethod = "refundCancel")
    Boolean refundTry(
            @BusinessActionContextParameter(paramName = "operateIntergralVo") OperateIntergralVo vo,
            BusinessActionContext context);

    Boolean refundConfirm(BusinessActionContext context);

    Boolean refundCancel(BusinessActionContext context);
}
