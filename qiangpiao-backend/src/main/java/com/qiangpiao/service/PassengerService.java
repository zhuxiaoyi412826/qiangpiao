package com.qiangpiao.service;

import com.qiangpiao.dto.PassengerDTO;
import com.qiangpiao.vo.PassengerVO;

import java.util.List;

/**
 * 常用乘车人服务（每个用户最多 3 位）。
 *
 * <pre>
 *   接口层：身份证只出脱敏值（前 4 后 4）；
 *   存储层：身份证 AES 加密落库；
 *   下单时：前端只回传 passengerId，由服务端解密取明文，避免证件号在前端反复明文传输。
 * </pre>
 */
public interface PassengerService {

    /** 常用乘车人最大数量：一次最多可买 9 张票，所以乘车人也放宽到 9 位 */
    int MAX_COUNT = 9;

    /**
     * 当前用户的常用乘车人列表（身份证脱敏）。
     */
    List<PassengerVO> list(Long userId);

    /**
     * 新增常用乘车人。
     *
     * @return 新记录主键
     */
    Long add(Long userId, PassengerDTO dto);

    /** 修改常用乘车人 */
    void update(Long userId, Long passengerId, PassengerDTO dto);

    /** 删除常用乘车人 */
    void delete(Long userId, Long passengerId);

    /**
     * 供下单使用：取本人常用乘车人的明文（姓名 + 身份证），不属于本人则报错。
     */
    PlainPassenger resolve(Long userId, Long passengerId);

    /** 下单用：明文乘客信息（仅内存流转，不落日志、不出接口） */
    class PlainPassenger {
        private final String passengerName;
        private final String idCard;

        public PlainPassenger(String passengerName, String idCard) {
            this.passengerName = passengerName;
            this.idCard = idCard;
        }

        public String getPassengerName() {
            return passengerName;
        }

        public String getIdCard() {
            return idCard;
        }
    }
}
