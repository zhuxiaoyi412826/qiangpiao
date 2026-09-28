package com.qiangpiao.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 退票入参。
 */
@Data
public class RefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private String reason;
}
