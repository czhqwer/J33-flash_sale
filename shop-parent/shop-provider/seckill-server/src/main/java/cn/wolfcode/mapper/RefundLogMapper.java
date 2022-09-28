package cn.wolfcode.mapper;

import cn.wolfcode.domain.RefundLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created by wolfcode-lanxw
 */
@Mapper
public interface RefundLogMapper {
    /**
     * 插入退款日志，用于幂等性控制
     * @param refundLog
     * @return
     */
    int insert(RefundLog refundLog);
}
