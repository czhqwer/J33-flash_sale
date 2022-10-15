package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountLog;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountLogMapper;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;


@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Resource
    private UsableIntegralMapper usableIntegralMapper;
    @Resource
    private AccountTransactionMapper accountTransactionMapper;
    @Resource
    private AccountLogMapper accountLogMapper;

    @Override
    @Transactional
    public Boolean pay(OperateIntergralVo vo) {
        //增加一条交易日志
        AccountLog accountLog = new AccountLog();
        accountLog.setPkValue(vo.getPk()); //主键1
        accountLog.setType(AccountLog.TYPE_DECR); //主键2
        accountLog.setAmount(vo.getValue());
        accountLog.setGmtTime(new Date());
        accountLog.setInfo(vo.getInfo());
        accountLogMapper.insert(accountLog);

        //减少积分
        int m = usableIntegralMapper.descIntergral(vo.getUserId(), vo.getValue());

        return m > 0;
    }

    @Override
    public Boolean refundTry(OperateIntergralVo vo, BusinessActionContext context) {
        /*+ null（正常逻辑） try（幂等） confirm（异常） cancel（什么都不做，空回滚）*/
        Boolean ret = false;
        //查询是否有事务日志
        //没有日志
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), String.valueOf(context.getBranchId()));
        if (accountTransaction == null) {
            //插入事务日志未try状态
            AccountTransaction transaction = new AccountTransaction();
            transaction.setTxId(context.getXid());
            transaction.setActionId(String.valueOf(context.getBranchId()));
            transaction.setUserId(vo.getUserId());
            transaction.setGmtCreated(new Date());
            transaction.setGmtModified(new Date());
            transaction.setAmount(vo.getValue());
            transaction.setType(String.valueOf(AccountLog.TYPE_INCR));
            transaction.setState(AccountTransaction.STATE_TRY);
            accountTransactionMapper.insert(transaction);
            //具体业务逻辑
            //无
            ret = true;
        } else if (AccountTransaction.STATE_TRY == accountTransaction.getState()) {
            //如果有日志，type是try -- 幂等
            ret = true;
        } else if (AccountTransaction.STATE_COMMIT == accountTransaction.getState()) {
            //如果有日志，type是confirm -- 异常
            throw new BusinessException(IntergralCodeMsg.OP_INTERGRAL_ERROR);
        } else {
            //如果有日志，type是cancel -- 防悬挂
            ret = false;
        }
        return ret;
    }

    @Override
    public Boolean refundConfirm(BusinessActionContext context) {
        /*+ null（异常） try（业务） confirm（幂等） cancel（异常）*/
        Boolean ret = false;
        String voJson = context.getActionContext().get("operateIntergralVo").toString();
        OperateIntergralVo vo = JSON.parseObject(voJson, OperateIntergralVo.class);
        //查询是否有事务日志
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), String.valueOf(context.getBranchId()));
        if (accountTransaction == null) { //null -- 异常
            throw new BusinessException(IntergralCodeMsg.OP_INTERGRAL_ERROR);
        } else if (AccountTransaction.STATE_TRY == accountTransaction.getState()) { // try -- 业务
            accountTransactionMapper.updateAccountTransactionState(
                    accountTransaction.getTxId(),
                    String.valueOf(context.getBranchId()),
                    AccountTransaction.STATE_COMMIT,
                    AccountTransaction.STATE_TRY);
            //交易日志
            AccountLog accountLog = new AccountLog();
            accountLog.setPkValue(vo.getPk()); //主键1
            accountLog.setType(AccountLog.TYPE_INCR); //主键2
            accountLog.setAmount(vo.getValue());
            accountLog.setGmtTime(new Date());
            accountLog.setInfo(vo.getInfo());
            accountLogMapper.insert(accountLog);
            //增加积分
            usableIntegralMapper.addIntergral(vo.getUserId(), vo.getValue());
            ret = true;
        } else if (AccountTransaction.STATE_COMMIT == accountTransaction.getState()) { // confirm -- 幂等
            ret = true;
        } else { // cancel -- 异常
            throw new BusinessException(IntergralCodeMsg.OP_INTERGRAL_ERROR);
        }
        return ret;
    }

    @Override
    public Boolean refundCancel(BusinessActionContext context) {
        Boolean ret = false;
        String voJson = context.getActionContext().get("operateIntergralVo").toString();
        OperateIntergralVo vo = JSON.parseObject(voJson, OperateIntergralVo.class);
        //查询是否有事务日志
        //没有日志
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), String.valueOf(context.getBranchId()));
        if (accountTransaction == null) { //null（防悬挂，空回滚）
            //insert
            //插入事务日志未cancel状态
            AccountTransaction transaction = new AccountTransaction();
            transaction.setTxId(context.getXid());
            transaction.setActionId(String.valueOf(context.getBranchId()));
            transaction.setUserId(vo.getUserId());
            transaction.setGmtCreated(new Date());
            transaction.setGmtModified(new Date());
            transaction.setAmount(vo.getValue());
            transaction.setType(String.valueOf(AccountLog.TYPE_INCR));
            transaction.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(transaction);
            ret = true;
        } else if (AccountTransaction.STATE_TRY == accountTransaction.getState()) {
            //try（业务）
            accountTransactionMapper.updateAccountTransactionState(
                    accountTransaction.getTxId(),
                    String.valueOf(context.getBranchId()),
                    AccountTransaction.STATE_CANCEL,
                    AccountTransaction.STATE_TRY);
            ret = true;
        } else if (AccountTransaction.STATE_COMMIT == accountTransaction.getState()) {
            //confirm（异常）
            throw new BusinessException(IntergralCodeMsg.OP_INTERGRAL_ERROR);
        } else {
            //cancel（幂等）
            ret = true;
        }
        return null;
    }

    //@Override
    public Boolean refund(OperateIntergralVo vo) {
        //交易日志
        AccountLog accountLog = new AccountLog();
        accountLog.setPkValue(vo.getPk()); //主键1
        accountLog.setType(AccountLog.TYPE_INCR); //主键2
        accountLog.setAmount(vo.getValue());
        accountLog.setGmtTime(new Date());
        accountLog.setInfo(vo.getInfo());
        accountLogMapper.insert(accountLog);
        //增加积分
        usableIntegralMapper.addIntergral(vo.getUserId(), vo.getValue());
        return true;
    }
}
