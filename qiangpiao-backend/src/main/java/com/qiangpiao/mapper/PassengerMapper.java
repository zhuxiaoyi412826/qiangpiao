package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.PassengerDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 常用乘车人 Mapper。
 */
@Repository
public interface PassengerMapper {

    @Select("SELECT id, user_id, passenger_name, id_card, phone, passenger_type, create_time, update_time"
            + " FROM t_passenger WHERE user_id = #{userId} ORDER BY id")
    List<PassengerDO> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id, passenger_name, id_card, phone, passenger_type, create_time, update_time"
            + " FROM t_passenger WHERE id = #{id}")
    PassengerDO selectById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM t_passenger WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    /** 同一用户下身份证去重：密文是确定性加密，可直接等值比较 */
    @Select("<script>SELECT COUNT(1) FROM t_passenger WHERE user_id = #{userId}"
            + " AND id_card = #{idCard}"
            + "<if test=\"excludeId != null\">AND id &lt;&gt; #{excludeId}</if></script>")
    long countByIdCard(@Param("userId") Long userId, @Param("idCard") String idCard,
                       @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO t_passenger (user_id, passenger_name, id_card, phone, passenger_type,"
            + " create_time, update_time)"
            + " VALUES (#{userId}, #{passengerName}, #{idCard}, #{phone}, #{passengerType}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PassengerDO passenger);

    @Update("UPDATE t_passenger SET passenger_name = #{passengerName}, id_card = #{idCard},"
            + " phone = #{phone}, passenger_type = #{passengerType}, update_time = NOW()"
            + " WHERE id = #{id} AND user_id = #{userId}")
    int update(PassengerDO passenger);

    @Delete("DELETE FROM t_passenger WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
