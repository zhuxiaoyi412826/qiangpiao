package com.qiangpiao.service;

import com.qiangpiao.bo.RiskCheckBO;

import java.util.List;

/**
 * 抢票风控：识别机器行为并留痕。
 *
 * <pre>
 *   1) 同一 IP 多账号：窗口内同一出口 IP 关联的账号数超过阈值 → 命中
 *   2) 极短耗时请求：同一账号两次抢票间隔小于阈值 → 疑似脚本，命中
 *   3) 命中留痕：累计次数写入 Redis，最近 N 条事件供后台查看
 *   4) 自动拉黑：累计命中达到阈值 → 自动拉黑该 IP
 * </pre>
 * 命中结果不直接拒绝（避免误伤），而是强制要求人机验证 + 累计后拉黑。
 */
public interface RiskControlService {

    /**
     * 抢票前置风控检查。
     *
     * @param userId 用户 ID
     * @param ip     客户端 IP
     */
    RiskCheckBO check(Long userId, String ip);

    /** 记录一条风控事件（后台可查） */
    void record(String ip, Long userId, String detail);

    /** 最近的风控事件（按时间倒序） */
    List<String> recentEvents(int limit);

    /** 某维度（IP）累计命中次数 */
    long hits(String dim);

    /** 清空某维度的命中计数（解封时用） */
    void resetHits(String dim);
}
