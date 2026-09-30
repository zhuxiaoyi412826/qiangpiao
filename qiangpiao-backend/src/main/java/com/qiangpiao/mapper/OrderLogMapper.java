package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.OrderLogDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单流转日志 Mapper（t_order_log）。
 * <p>
 * 原来这两个 SQL 放在 AdminMapper 里，日志属于订单域而非后台域，单独拆出来供各业务 Service 复用。
 */
@Repository
public interface OrderLogMapper {

    @Insert("INSERT INTO t_order_log (order_no, action, action_text, detail, operator, trace_id)" +
            " VALUES (#{orderNo}, #{action}, #{actionText}, #{detail}, #{operator}, #{traceId})")
    int insert(OrderLogDO log);

    /** 单个订单的完整流转时间轴 */
    @Select("SELECT id, order_no, action, action_text, detail, operator, trace_id, create_time" +
            " FROM t_order_log WHERE order_no = #{orderNo} ORDER BY id")
    List<OrderLogDO> selectByOrderNo(@Param("orderNo") String orderNo);

    /** 批量订单的日志（列表页批量展示，避免 N+1） */
    @Select("<script>SELECT id, order_no, action, action_text, detail, operator, trace_id, create_time" +
            " FROM t_order_log WHERE order_no IN" +
            "<foreach collection='orderNos' item='no' open='(' separator=',' close=')'>#{no}</foreach>" +
            " ORDER BY id</script>")
    List<OrderLogDO> selectByOrderNos(@Param("orderNos") List<String> orderNos);
}
