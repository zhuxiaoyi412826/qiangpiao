package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.dto.CaptchaVerifyDTO;
import com.qiangpiao.service.CaptchaService;
import com.qiangpiao.vo.CaptchaVO;
import com.qiangpiao.vo.SliderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 人机验证实现：纯 JDK AWT 绘制，无第三方依赖。
 * <pre>
 *   图形验证码：4 位字符（去掉易混淆的 0/1/I/O），干扰线 + 噪点 + 字符旋转
 *   滑块验证码：随机渐变背景，随机位置挖出拼图块，缺口坐标存 Redis，前端只拿到图片
 * </pre>
 * 验证码一律一次性：校验通过 / 失败后立即删除，防止爆破重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    /** 验证码有效期（分钟） */
    private static final long TTL_MINUTES = 5;
    /** 图形验证码字符集：去掉 0/1/I/O 等易混淆字符 */
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int IMG_WIDTH = 128;
    private static final int IMG_HEIGHT = 44;
    private static final int CODE_LENGTH = 4;

    /** 滑块画布 */
    private static final int BG_WIDTH = 320;
    private static final int BG_HEIGHT = 160;
    private static final int BLOCK_SIZE = 52;
    /** 滑块允许误差（px）：真人拖动很难精准到 1px，放宽到 8 避免「看着对齐了却一直失败」 */
    private static final int TOLERANCE = 8;

    private final StringRedisTemplate stringRedisTemplate;

    static {
        // 服务器无图形环境时，AWT 图像处理仍可用
        System.setProperty("java.awt.headless", "true");
    }

    @Override
    public CaptchaVO generateImage() {
        String code = randomCode();
        String id = newId();
        Random random = new Random();

        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(244, 248, 255));
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
        // 噪点
        for (int i = 0; i < 80; i++) {
            g.setColor(new Color(150 + random.nextInt(100), 160 + random.nextInt(90), 190 + random.nextInt(60)));
            g.fillOval(random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT), 2, 2);
        }
        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(110 + random.nextInt(110), 130 + random.nextInt(110), 170 + random.nextInt(80)));
            g.drawLine(random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT),
                    random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT));
        }
        // 字符：逐个随机颜色 + 轻微旋转
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        for (int i = 0; i < code.length(); i++) {
            AffineTransform origin = g.getTransform();
            int x = 12 + i * 28;
            int y = 32 + random.nextInt(7);
            g.setColor(new Color(20 + random.nextInt(130), 40 + random.nextInt(120), 110 + random.nextInt(120)));
            g.rotate(Math.toRadians(-14 + random.nextInt(28)), x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.setTransform(origin);
        }
        g.dispose();

        String base64 = toBase64(image);
        save(RedisKeys.captcha(id), code);
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(id);
        vo.setImage(base64);
        vo.setExpireSeconds(TTL_MINUTES * 60);
        return vo;
    }

    @Override
    public SliderVO generateSlider() {
        String id = newId();
        Random random = new Random();

        BufferedImage background = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = background.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(205, 226, 250), BG_WIDTH, BG_HEIGHT, new Color(120, 172, 226)));
        g.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);
        for (int i = 0; i < 10; i++) {
            g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255), 80));
            int size = 20 + random.nextInt(60);
            g.fillOval(random.nextInt(BG_WIDTH - size), random.nextInt(BG_HEIGHT - size), size, size);
        }
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(255, 255, 255, 70));
            g.drawLine(random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT),
                    random.nextInt(BG_WIDTH), random.nextInt(BG_HEIGHT));
        }
        g.dispose();

        int targetX = 50 + random.nextInt(BG_WIDTH - BLOCK_SIZE - 70);
        int targetY = 15 + random.nextInt(BG_HEIGHT - BLOCK_SIZE - 30);

        // 拼图块：从背景裁切 + 高光 + 圆角描边
        BufferedImage block = new BufferedImage(BLOCK_SIZE, BLOCK_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg2 = block.createGraphics();
        bg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg2.drawImage(background.getSubimage(targetX, targetY, BLOCK_SIZE, BLOCK_SIZE), 0, 0, null);
        bg2.setColor(new Color(255, 255, 255, 90));
        bg2.fillOval(10, 10, BLOCK_SIZE - 20, BLOCK_SIZE - 20);
        bg2.setColor(new Color(255, 255, 255, 170));
        bg2.drawRoundRect(0, 0, BLOCK_SIZE - 1, BLOCK_SIZE - 1, 10, 10);
        bg2.dispose();

        // 背景缺口：暗化 + 白色描边
        Graphics2D g3 = background.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(new Color(0, 0, 0, 120));
        g3.fillRoundRect(targetX, targetY, BLOCK_SIZE, BLOCK_SIZE, 10, 10);
        g3.setColor(new Color(255, 255, 255, 180));
        g3.drawRoundRect(targetX, targetY, BLOCK_SIZE, BLOCK_SIZE, 10, 10);
        g3.dispose();

        save(RedisKeys.slider(id), String.valueOf(targetX));
        SliderVO vo = new SliderVO();
        vo.setSliderId(id);
        vo.setBackground(toBase64(background));
        vo.setBlock(toBase64(block));
        vo.setBlockY(targetY);
        vo.setBlockWidth(BLOCK_SIZE);
        vo.setBlockHeight(BLOCK_SIZE);
        vo.setWidth(BG_WIDTH);
        vo.setHeight(BG_HEIGHT);
        vo.setExpireSeconds(TTL_MINUTES * 60);
        return vo;
    }

    @Override
    public boolean verifyImage(String captchaId, String code) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = RedisKeys.captcha(captchaId);
        String expect = take(key);
        return expect != null && expect.equalsIgnoreCase(code.trim());
    }

    @Override
    public boolean verifySlider(String sliderId, Integer x) {
        if (!StringUtils.hasText(sliderId) || x == null) {
            return false;
        }
        String key = RedisKeys.slider(sliderId);
        String expect = take(key);
        if (expect == null) {
            return false;
        }
        try {
            return Math.abs(Integer.parseInt(expect) - x) <= TOLERANCE;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean verify(CaptchaVerifyDTO dto) {
        if (dto == null) {
            return false;
        }
        if (StringUtils.hasText(dto.getCaptchaId())) {
            return verifyImage(dto.getCaptchaId(), dto.getCaptchaCode());
        }
        if (StringUtils.hasText(dto.getSliderId())) {
            return verifySlider(dto.getSliderId(), dto.getSliderX());
        }
        return false;
    }

    /** 取出并立即删除：验证码一次性 */
    private String take(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            stringRedisTemplate.delete(key);
            return value;
        } catch (Exception e) {
            log.warn("验证码读取失败：key={}, msg={}", key, e.getMessage());
            return null;
        }
    }

    private void save(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("验证码写入 Redis 失败：msg={}", e.getMessage());
        }
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String toBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            log.error("验证码图片生成失败：msg={}", e.getMessage());
            return "";
        }
    }
}
