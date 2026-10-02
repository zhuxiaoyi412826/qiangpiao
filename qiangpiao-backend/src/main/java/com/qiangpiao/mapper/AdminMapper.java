package com.qiangpiao.mapper;

import com.qiangpiao.dataobject.AnnouncementDO;
import com.qiangpiao.dataobject.CarriageDO;
import com.qiangpiao.dataobject.LineDO;
import com.qiangpiao.dataobject.LineStationDO;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.StationDO;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dataobject.TrainStopDO;
import com.qiangpiao.dataobject.UserDO;
import com.qiangpiao.dto.AdminOrderQueryDTO;
import com.qiangpiao.vo.AdminOrderVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 Mapper：车站 / 线路 / 车次（时刻表·车厢·停运·排班）/ 订单 / 用户 / 公告 / 统计。
 * 后台读写频率低，统一用注解 SQL，避免为每个模块单独建 XML。
 */
@Repository
public interface AdminMapper {

    // ==================== 车站 ====================

    @Select("SELECT id, station_name, city, py_code, status, create_time FROM t_station ORDER BY id")
    List<StationDO> listStations();

    @Select("SELECT id, station_name, city, py_code, status, create_time FROM t_station WHERE status = 1 ORDER BY id")
    List<StationDO> listActiveStations();

    @Insert("INSERT INTO t_station (station_name, city, py_code, status) VALUES (#{stationName}, #{city}, #{pyCode}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStation(StationDO station);

    @Update("UPDATE t_station SET station_name = #{stationName}, city = #{city}, py_code = #{pyCode} WHERE id = #{id}")
    int updateStation(StationDO station);

    @Update("UPDATE t_station SET status = #{status} WHERE id = #{id}")
    int updateStationStatus(@Param("id") Long id, @Param("status") Integer status);

    // ==================== 线路 ====================

    @Select("SELECT id, line_name, from_station_id, from_station_name, to_station_id, to_station_name, status," +
            " create_time, update_time FROM t_line ORDER BY id DESC")
    List<LineDO> listLines();

    @Insert("INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)" +
            " VALUES (#{lineName}, #{fromStationId}, #{fromStationName}, #{toStationId}, #{toStationName}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLine(LineDO line);

    @Update("UPDATE t_line SET line_name = #{lineName}, from_station_id = #{fromStationId}," +
            " from_station_name = #{fromStationName}, to_station_id = #{toStationId}," +
            " to_station_name = #{toStationName}, status = #{status} WHERE id = #{id}")
    int updateLine(LineDO line);

    @Delete("DELETE FROM t_line WHERE id = #{id}")
    int deleteLine(Long id);

    @Delete("DELETE FROM t_line_station WHERE line_id = #{lineId}")
    int deleteLineStations(Long lineId);

    @Select("SELECT id, line_id, station_id, station_name, stop_order FROM t_line_station" +
            " WHERE line_id = #{lineId} ORDER BY stop_order")
    List<LineStationDO> listLineStations(Long lineId);

    @Insert("INSERT INTO t_line_station (line_id, station_id, station_name, stop_order)" +
            " VALUES (#{lineId}, #{stationId}, #{stationName}, #{stopOrder})")
    int insertLineStation(LineStationDO station);

    // ==================== 车次 / 时刻表 / 车厢 / 排班 ====================

    @Select("SELECT id, train_no, train_type, from_station_id, from_station_name, to_station_id, to_station_name," +
            " depart_date, depart_time, arrive_time, duration_minutes, status, create_time, update_time" +
            " FROM t_train ORDER BY depart_date DESC, depart_time LIMIT #{offset}, #{limit}")
    List<TrainDO> listTrains(@Param("offset") Long offset, @Param("limit") Long limit);

    @Select("SELECT COUNT(1) FROM t_train")
    long countTrains();

    @Update("UPDATE t_train SET status = #{status} WHERE id = #{id}")
    int updateTrainStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 设置车次售卖时间窗口；两个参数都可为空（不限时） */
    @Update("UPDATE t_train SET sale_start_time = #{startTime}, sale_end_time = #{endTime} WHERE id = #{id}")
    int updateSaleWindow(@Param("id") Long id, @Param("startTime") java.time.LocalDateTime startTime,
                         @Param("endTime") java.time.LocalDateTime endTime);

    @Select("SELECT id, train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes," +
            " distance_km FROM t_train_stop WHERE train_id = #{trainId} ORDER BY stop_order")
    List<TrainStopDO> listStops(Long trainId);

    @Delete("DELETE FROM t_train_stop WHERE train_id = #{trainId}")
    int deleteStops(Long trainId);

    @Insert("INSERT INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time," +
            " stop_minutes, distance_km) VALUES (#{trainId}, #{stationId}, #{stationName}, #{stopOrder}," +
            " #{arriveTime}, #{departTime}, #{stopMinutes}, #{distanceKm})")
    int insertStop(TrainStopDO stop);

    /**
     * 复制时刻表到新生成的每日班次。
     * 少了这一步，新班次就没有经停站数据：中转方案建不出图、区间票也全退化成全程票。
     */
    @Insert("INSERT INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time," +
            " stop_minutes, distance_km)" +
            " SELECT #{toTrainId}, station_id, station_name, stop_order, arrive_time, depart_time," +
            " stop_minutes, distance_km FROM t_train_stop WHERE train_id = #{fromTrainId}")
    int copyStops(@Param("fromTrainId") Long fromTrainId, @Param("toTrainId") Long toTrainId);

    @Select("SELECT id, train_id, carriage_no, seat_type, seat_count, create_time FROM t_carriage" +
            " WHERE train_id = #{trainId} ORDER BY carriage_no")
    List<CarriageDO> listCarriages(Long trainId);

    @Insert("INSERT INTO t_carriage (train_id, carriage_no, seat_type, seat_count)" +
            " VALUES (#{trainId}, #{carriageNo}, #{seatType}, #{seatCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCarriage(CarriageDO carriage);

    @Delete("DELETE FROM t_carriage WHERE train_id = #{trainId}")
    int deleteCarriages(Long trainId);

    /** 按日期复制出一个新的当日班次（车次模板 -> 每日排班） */
    @Insert("INSERT INTO t_train (train_no, train_type, from_station_id, from_station_name, to_station_id," +
            " to_station_name, depart_date, depart_time, arrive_time, duration_minutes, status," +
            " sale_start_time, sale_end_time)" +
            " SELECT train_no, train_type, from_station_id, from_station_name, to_station_id, to_station_name," +
            " #{newDate}, depart_time, arrive_time, duration_minutes, status, sale_start_time, sale_end_time" +
            " FROM t_train WHERE id = #{sourceTrainId}")
    int copyTrain(@Param("sourceTrainId") Long sourceTrainId, @Param("newDate") LocalDate newDate);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Insert("INSERT INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)" +
            " SELECT #{newTrainId}, seat_type, total_count, total_count, price, 0 FROM t_train_stock" +
            " WHERE train_id = #{sourceTrainId}")
    int copyStock(@Param("sourceTrainId") Long sourceTrainId, @Param("newTrainId") Long newTrainId);

    @Insert("INSERT INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)" +
            " SELECT #{newTrainId}, seat_type, carriage_no, seat_no, 0, 0 FROM t_seat WHERE train_id = #{sourceTrainId}")
    int copySeats(@Param("sourceTrainId") Long sourceTrainId, @Param("newTrainId") Long newTrainId);

    // ==================== 订单（后台） ====================

    @Select("<script>" +
            "SELECT o.id, o.order_no, o.user_id, o.train_id, o.seat_type, o.carriage_no, o.seat_no," +
            " o.passenger_name, o.id_card, o.price, o.status, o.depart_date, o.create_time, o.pay_time," +
            " o.cancel_time, o.expire_time, u.username, u.phone," +
            " COALESCE(o.train_no_snapshot, t.train_no) AS train_no," +
            " COALESCE(o.from_station_snapshot, t.from_station_name) AS from_station_name," +
            " COALESCE(o.to_station_snapshot, t.to_station_name) AS to_station_name," +
            " COALESCE(o.depart_time_snapshot, t.depart_time) AS depart_time" +
            " FROM t_order o LEFT JOIN t_user u ON u.id = o.user_id LEFT JOIN t_train t ON t.id = o.train_id" +
            "<where>" +
            "<if test=\"orderNo != null and orderNo != ''\">AND o.order_no = #{orderNo}</if>" +
            // 手机号已加密：密文等值匹配（phoneCipher），保留明文等值以兼容存量未加密数据
            "<if test=\"phone != null and phone != ''\">AND (u.phone = #{phoneCipher} OR u.phone = #{phone})</if>" +
            "<if test=\"passengerName != null and passengerName != ''\">AND o.passenger_name LIKE CONCAT('%', #{passengerName}, '%')</if>" +
            "<if test=\"status != null\">AND o.status = #{status}</if>" +
            "<if test=\"startDate != null\">AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null\">AND o.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "</where> ORDER BY o.id DESC LIMIT #{offset}, #{limit}</script>")
    List<AdminOrderVO> listOrders(AdminOrderQueryDTO query);

    @Select("<script>SELECT COUNT(1) FROM t_order o LEFT JOIN t_user u ON u.id = o.user_id" +
            "<where>" +
            "<if test=\"orderNo != null and orderNo != ''\">AND o.order_no = #{orderNo}</if>" +
            // 手机号已加密：密文等值匹配（phoneCipher），保留明文等值以兼容存量未加密数据
            "<if test=\"phone != null and phone != ''\">AND (u.phone = #{phoneCipher} OR u.phone = #{phone})</if>" +
            "<if test=\"passengerName != null and passengerName != ''\">AND o.passenger_name LIKE CONCAT('%', #{passengerName}, '%')</if>" +
            "<if test=\"status != null\">AND o.status = #{status}</if>" +
            "<if test=\"startDate != null\">AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null\">AND o.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "</where></script>")
    long countOrders(AdminOrderQueryDTO query);

    @Update("UPDATE t_order SET status = #{status}, cancel_time = NOW() WHERE order_no = #{orderNo}")
    int updateOrderStatus(@Param("orderNo") String orderNo, @Param("status") Integer status);

    // ==================== 订单流转日志 / 改签 ====================

    @Insert("INSERT INTO t_order_change (order_no, new_order_no, user_id, old_train_id, new_train_id," +
            " old_seat_no, new_seat_no, diff_amount, change_fee, fee_rule, reason)" +
            " VALUES (#{orderNo}, #{newOrderNo}, #{userId}, #{oldTrainId}, #{newTrainId}," +
            " #{oldSeatNo}, #{newSeatNo}, #{diffAmount}, #{changeFee}, #{feeRule}, #{reason})")
    int insertOrderChange(OrderChangeDO change);

    @Select("SELECT id, order_no, new_order_no, user_id, old_train_id, new_train_id, old_seat_no, new_seat_no," +
            " diff_amount, change_fee, fee_rule, reason, create_time FROM t_order_change" +
            " WHERE order_no = #{orderNo} OR new_order_no = #{orderNo} ORDER BY id DESC")
    List<OrderChangeDO> listOrderChanges(String orderNo);

    // ==================== 用户管理 ====================

    // 手机号加密后无法模糊匹配：keyword 命中手机号时改用密文等值（phoneCipher），
    // 同时保留明文 LIKE 以兼容存量未加密数据
    @Select("<script>SELECT id, username, real_name, phone, id_card, role, status, create_time FROM t_user" +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\">AND (username LIKE CONCAT('%', #{keyword}, '%')" +
            " OR phone LIKE CONCAT('%', #{keyword}, '%')" +
            "<if test=\"phoneCipher != null\"> OR phone = #{phoneCipher}</if>)</if>" +
            "</where> ORDER BY id DESC LIMIT #{offset}, #{limit}</script>")
    List<UserDO> listUsers(@Param("keyword") String keyword, @Param("phoneCipher") String phoneCipher,
                           @Param("offset") Long offset, @Param("limit") Long limit);

    @Select("<script>SELECT COUNT(1) FROM t_user" +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\">AND (username LIKE CONCAT('%', #{keyword}, '%')" +
            " OR phone LIKE CONCAT('%', #{keyword}, '%')" +
            "<if test=\"phoneCipher != null\"> OR phone = #{phoneCipher}</if>)</if>" +
            "</where></script>")
    long countUsers(@Param("keyword") String keyword, @Param("phoneCipher") String phoneCipher);

    @Update("UPDATE t_user SET status = #{status} WHERE id = #{id}")
    int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

    // ==================== 公告 ====================

    @Select("SELECT id, title, content, type, status, create_time, update_time FROM t_announcement" +
            " ORDER BY status DESC, create_time DESC LIMIT #{offset}, #{limit}")
    List<AnnouncementDO> listAnnouncements(@Param("offset") Long offset, @Param("limit") Long limit);

    @Select("SELECT COUNT(1) FROM t_announcement")
    long countAnnouncements();

    @Select("SELECT id, title, content, type, status, create_time, update_time FROM t_announcement" +
            " WHERE status = 1 ORDER BY create_time DESC LIMIT #{limit}")
    List<AnnouncementDO> listActiveAnnouncements(Long limit);

    @Insert("INSERT INTO t_announcement (title, content, type, status)" +
            " VALUES (#{title}, #{content}, #{type}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAnnouncement(AnnouncementDO announcement);

    @Update("UPDATE t_announcement SET title = #{title}, content = #{content}, type = #{type}," +
            " status = #{status} WHERE id = #{id}")
    int updateAnnouncement(AnnouncementDO announcement);

    @Delete("DELETE FROM t_announcement WHERE id = #{id}")
    int deleteAnnouncement(Long id);

    // ==================== 票务监控 / 统计报表 ====================

    // 库存 8000+ 行（2700 班次 × 3 席别），必须分页，否则后台首屏一次性拉全表会卡死页面
    @Select("<script>SELECT s.id AS id, t.id AS trainId, t.train_no AS trainNo, t.depart_date AS departDate," +
            " s.seat_type AS seatType, s.total_count AS totalCount, s.available_count AS availableCount," +
            " s.price AS price FROM t_train_stock s INNER JOIN t_train t ON t.id = s.train_id" +
            "<where>" +
            "<if test=\"trainNo != null and trainNo != ''\">AND t.train_no LIKE CONCAT('%', #{trainNo}, '%')</if>" +
            "</where>" +
            " ORDER BY t.depart_date, t.train_no, s.seat_type LIMIT #{offset}, #{limit}</script>")
    List<Map<String, Object>> stockMonitor(@Param("trainNo") String trainNo,
                                           @Param("offset") long offset, @Param("limit") long limit);

    @Select("<script>SELECT COUNT(1) FROM t_train_stock s INNER JOIN t_train t ON t.id = s.train_id" +
            "<where>" +
            "<if test=\"trainNo != null and trainNo != ''\">AND t.train_no LIKE CONCAT('%', #{trainNo}, '%')</if>" +
            "</where></script>")
    long countStockMonitor(@Param("trainNo") String trainNo);

    @Select("SELECT id, train_id, seat_type, carriage_no, seat_no, status, order_no, update_time FROM t_seat" +
            " WHERE status = 2 ORDER BY update_time DESC LIMIT #{limit}")
    List<SeatDO> listLockedSeats(Long limit);

    @Select("SELECT DATE(o.create_time) AS statDate, COUNT(1) AS orderCount, IFNULL(SUM(o.price), 0) AS amount" +
            " FROM t_order o WHERE o.status IN (1, 3) GROUP BY DATE(o.create_time)" +
            " ORDER BY statDate DESC LIMIT 30")
    List<Map<String, Object>> dailySales();

    @Select("SELECT t.train_no AS trainNo, t.depart_date AS departDate, COUNT(o.id) AS soldCount," +
            " IFNULL(SUM(o.price), 0) AS amount FROM t_train t LEFT JOIN t_order o" +
            " ON o.train_id = t.id AND o.status = 1 GROUP BY t.id ORDER BY soldCount DESC LIMIT 50")
    List<Map<String, Object>> trainSales();

    @Select("SELECT IFNULL(SUM(CASE WHEN type = 2 THEN -amount ELSE 0 END), 0) AS income," +
            " IFNULL(SUM(CASE WHEN type = 3 THEN amount ELSE 0 END), 0) AS refundAmount," +
            " IFNULL(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS rechargeAmount FROM t_wallet_flow")
    Map<String, Object> revenue();

    @Select("SELECT COUNT(1) AS total, SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS banned FROM t_user")
    Map<String, Object> userSummary();

    @Select("SELECT DATE(create_time) AS statDate, COUNT(1) AS newCount FROM t_user" +
            " GROUP BY DATE(create_time) ORDER BY statDate DESC LIMIT 30")
    List<Map<String, Object>> userGrowth();

    // ==================== 退票 / 改签（售后） ====================

    @Select("SELECT id, train_id, seat_type, total_count, available_count, price, version FROM t_train_stock" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType}")
    TrainStockDO stock(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    /** 用 MySQL 的原子 UPDATE ... LIMIT 1 选座，避免并发重复分配同一个座位 */
    @Update("UPDATE t_seat SET status = 1, order_no = #{orderNo}" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType} AND status = 0 LIMIT 1")
    int occupySeat(@Param("trainId") Long trainId, @Param("seatType") Integer seatType,
                   @Param("orderNo") String orderNo);

    @Select("SELECT id, train_id, seat_type, carriage_no, seat_no, status, order_no FROM t_seat" +
            " WHERE train_id = #{trainId} AND order_no = #{orderNo} LIMIT 1")
    SeatDO seatOfOrder(@Param("trainId") Long trainId, @Param("orderNo") String orderNo);

    @Update("UPDATE t_seat SET status = 0, order_no = NULL WHERE order_no = #{orderNo}")
    int releaseSeat(String orderNo);

    // 归还库存必须带 available_count < total_count 条件，重复释放（幂等重试）不会把余票抬超总座数
    @Update("UPDATE t_train_stock SET available_count = LEAST(total_count, available_count + 1)" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType} AND available_count < total_count")
    int restoreStock(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    /** 票价调整：按车次席别改价 */
    @Update("UPDATE t_train_stock SET price = #{price} WHERE id = #{id}")
    int updateStockPrice(@Param("id") Long id, @Param("price") java.math.BigDecimal price);

    @Update("UPDATE t_train_stock SET total_count = #{totalCount}," +
            " available_count = LEAST(#{totalCount}, available_count + (#{totalCount} - total_count))" +
            " WHERE id = #{id}")
    int updateStockTotal(@Param("id") Long id, @Param("totalCount") Integer totalCount);

    @Update("UPDATE t_train_stock SET available_count = GREATEST(0, available_count - 1)" +
            " WHERE train_id = #{trainId} AND seat_type = #{seatType} AND available_count > 0")
    int deductStock(@Param("trainId") Long trainId, @Param("seatType") Integer seatType);

    @Update("UPDATE t_order SET train_id = #{trainId}, seat_type = #{seatType}, carriage_no = #{carriageNo}," +
            " seat_no = #{seatNo}, price = #{price}, depart_date = #{departDate}, changed = 1," +
            " train_no_snapshot = #{trainNo}, train_type_snapshot = #{trainType}," +
            " from_station_snapshot = #{fromStation}, to_station_snapshot = #{toStation}," +
            " depart_time_snapshot = #{departTime}, arrive_time_snapshot = #{arriveTime}," +
            " origin_depart_time = IFNULL(origin_depart_time, #{originDepartTime})" +
            " WHERE order_no = #{orderNo}")
    int updateOrderForChange(@Param("orderNo") String orderNo, @Param("trainId") Long trainId,
                             @Param("seatType") Integer seatType, @Param("carriageNo") Integer carriageNo,
                             @Param("seatNo") String seatNo, @Param("price") java.math.BigDecimal price,
                             @Param("departDate") LocalDate departDate,
                             @Param("trainNo") String trainNo, @Param("trainType") String trainType,
                             @Param("fromStation") String fromStation, @Param("toStation") String toStation,
                             @Param("departTime") java.time.LocalTime departTime,
                             @Param("arriveTime") java.time.LocalTime arriveTime,
                             @Param("originDepartTime") java.time.LocalDateTime originDepartTime);

    /** 退票落库：手续费与实退金额 */
    @Update("UPDATE t_order SET refund_fee = #{refundFee}, refund_amount = #{refundAmount}, cancel_time = NOW()" +
            " WHERE order_no = #{orderNo}")
    int updateOrderRefund(@Param("orderNo") String orderNo,
                          @Param("refundFee") java.math.BigDecimal refundFee,
                          @Param("refundAmount") java.math.BigDecimal refundAmount);
}
