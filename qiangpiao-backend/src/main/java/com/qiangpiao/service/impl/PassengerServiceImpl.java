package com.qiangpiao.service.impl;

import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.SensitiveCrypto;
import com.qiangpiao.dataobject.PassengerDO;
import com.qiangpiao.dto.PassengerDTO;
import com.qiangpiao.mapper.PassengerMapper;
import com.qiangpiao.service.PassengerService;
import com.qiangpiao.vo.PassengerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 常用乘车人服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;
    private final SensitiveCrypto crypto;

    @Override
    public List<PassengerVO> list(Long userId) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        List<PassengerDO> list = passengerMapper.selectByUserId(userId);
        List<PassengerVO> vos = new ArrayList<>();
        if (list != null) {
            for (PassengerDO p : list) {
                vos.add(PassengerVO.builder()
                        .id(p.getId())
                        .passengerName(p.getPassengerName())
                        .idCardMasked(crypto.maskIdCard(p.getIdCard()))
                        // 手机号同样是密文：解密后脱敏出接口
                        .phoneMasked(crypto.maskPhone(p.getPhone()))
                        .passengerType(p.getPassengerType())
                        .passengerTypeText(typeText(p.getPassengerType()))
                        .createTime(p.getCreateTime())
                        .build());
            }
        }
        return vos;
    }

    @Override
    public Long add(Long userId, PassengerDTO dto) {
        assertOwner(userId);
        assertDto(dto);
        long count = passengerMapper.countByUserId(userId);
        if (count >= MAX_COUNT) {
            throw new BizException(ResultCode.PASSENGER_LIMIT);
        }
        assertIdCardUnique(userId, crypto.encrypt(dto.getIdCard()), null);

        PassengerDO passenger = new PassengerDO();
        passenger.setUserId(userId);
        passenger.setPassengerName(dto.getPassengerName().trim());
        // 明文进，密文出
        passenger.setIdCard(crypto.encrypt(dto.getIdCard()));
        // 手机号明文传入，AES 加密落库（选填，为空不加密）
        passenger.setPhone(crypto.encrypt(dto.getPhone()));
        passenger.setPassengerType(dto.getPassengerType() == null ? 1 : dto.getPassengerType());
        passengerMapper.insert(passenger);
        log.info("新增常用乘车人：userId={}, passengerId={}", userId, passenger.getId());
        return passenger.getId();
    }

    @Override
    public void update(Long userId, Long passengerId, PassengerDTO dto) {
        assertOwner(userId);
        assertDto(dto);
        PassengerDO exist = loadOwn(userId, passengerId);
        assertIdCardUnique(userId, crypto.encrypt(dto.getIdCard()), passengerId);

        PassengerDO passenger = new PassengerDO();
        passenger.setId(passengerId);
        passenger.setUserId(userId);
        passenger.setPassengerName(dto.getPassengerName().trim());
        passenger.setIdCard(crypto.encrypt(dto.getIdCard()));
        // 手机号明文传入，AES 加密落库（选填，为空不加密）
        passenger.setPhone(crypto.encrypt(dto.getPhone()));
        passenger.setPassengerType(dto.getPassengerType() == null ? exist.getPassengerType() : dto.getPassengerType());
        passengerMapper.update(passenger);
    }

    @Override
    public void delete(Long userId, Long passengerId) {
        assertOwner(userId);
        loadOwn(userId, passengerId);
        passengerMapper.delete(passengerId, userId);
        log.info("删除常用乘车人：userId={}, passengerId={}", userId, passengerId);
    }

    @Override
    public PlainPassenger resolve(Long userId, Long passengerId) {
        PassengerDO p = loadOwn(userId, passengerId);
        // 密文解密成明文，只在内存流转，直接用于下单落库（落库时再次加密）
        return new PlainPassenger(p.getPassengerName(), crypto.decrypt(p.getIdCard()));
    }

    // ==================== private ====================

    private PassengerDO loadOwn(Long userId, Long passengerId) {
        if (passengerId == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        PassengerDO p = passengerMapper.selectById(passengerId);
        if (p == null || p.getUserId() == null || !p.getUserId().equals(userId)) {
            // 不区分「不存在」与「不是本人的」，避免被拿来探测他人乘车人
            throw new BizException(ResultCode.PASSENGER_NOT_FOUND);
        }
        return p;
    }

    private void assertOwner(Long userId) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    private void assertDto(PassengerDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getPassengerName()) || !StringUtils.hasText(dto.getIdCard())) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        // 身份证：位数 + 校验位 + 出生日期，乱填直接拒绝
        if (!crypto.validIdCard(dto.getIdCard().trim())) {
            throw new BizException(ResultCode.ID_CARD_INVALID);
        }
        // 手机号选填，填了就要合规（位数 + 号段）
        String phone = dto.getPhone();
        if (StringUtils.hasText(phone) && !crypto.validPhone(phone)) {
            throw new BizException(ResultCode.PHONE_INVALID);
        }
    }

    /** 同一用户下身份证不可重复（密文确定性加密，可直接等值比较） */
    private void assertIdCardUnique(Long userId, String cipher, Long excludeId) {
        if (passengerMapper.countByIdCard(userId, cipher, excludeId) > 0) {
            throw new BizException(ResultCode.PASSENGER_ID_CARD_EXISTS);
        }
    }

    private String typeText(Integer type) {
        if (type == null) {
            return "成人";
        }
        switch (type) {
            case 2:
                return "儿童";
            case 3:
                return "学生";
            case 4:
                return "残军";
            default:
                return "成人";
        }
    }
}
