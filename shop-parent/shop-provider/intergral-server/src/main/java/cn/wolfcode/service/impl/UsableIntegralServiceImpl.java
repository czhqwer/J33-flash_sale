package cn.wolfcode.service.impl;

import cn.wolfcode.domain.AccountLog;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountLogMapper;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
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
