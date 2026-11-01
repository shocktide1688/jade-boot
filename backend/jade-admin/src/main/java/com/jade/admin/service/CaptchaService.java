package com.jade.admin.service;

import com.jade.admin.dto.CaptchaResponse;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码服务（SVG 生成，无 AWT 依赖，JVM / Native 均可用）
 *
 * 答案存 Redis（TTL 120s、一次性），进程内始终双写：Redis 不可用时自动降级为
 * 进程内校验（单机语义：重启后旧验证码失效、多实例需会话粘性），功能不中断。
 * 校验不再 fail-open：降级存储查不到即拒绝（此前 Redis 故障会放行任何答案）。
 */
@ApplicationScoped
public class CaptchaService {

    private static final Logger LOG = Logger.getLogger(CaptchaService.class);

    public static final String KEY_PREFIX = "captcha:";
    /** 验证码有效期（秒） */
    private static final long TTL_SECONDS = 120;
    /** 排除易混淆字符 0O1I */
    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    /** Redis 故障后熔断时长，到期重试 */
    private static final long REDIS_RETRY_MS = 30_000;

    @Inject
    RedisDataSource redis;

    private final SecureRandom random = new SecureRandom();
    private final InMemoryTtlStore memory = new InMemoryTtlStore();
    /** 0 = Redis 可用；否则为下次允许重试 Redis 的时间戳 */
    private volatile long redisRetryAt;

    /** 生成验证码：返回 uuid + SVG 图片（data URI） */
    public CaptchaResponse generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        String fullKey = KEY_PREFIX + uuid;
        memory.setex(fullKey, TTL_SECONDS, code);
        if (redisAvailable()) {
            try {
                cmd().setex(fullKey, TTL_SECONDS, code);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        return new CaptchaResponse(uuid, render(code));
    }

    /**
     * 校验验证码（忽略大小写，一次性：无论对错立即作废，防同一 uuid 爆破）。
     * Redis 查不到（含降级期）再看进程内存储；两处都没有即拒绝，不放行。
     */
    public boolean verify(String uuid, String code) {
        if (uuid == null || uuid.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String fullKey = KEY_PREFIX + uuid;
        String expected = null;
        if (redisAvailable()) {
            try {
                expected = cmd().get(fullKey);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        if (expected == null) {
            expected = memory.get(fullKey);
        }
        if (redisAvailable()) {
            try {
                redis.key().del(fullKey);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        memory.del(fullKey);
        return expected != null && expected.equalsIgnoreCase(code.trim());
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    private ValueCommands<String, String> cmd() {
        return redis.value(String.class, String.class);
    }

    private boolean redisAvailable() {
        return System.currentTimeMillis() >= redisRetryAt;
    }

    private void markRedisDown(Exception e) {
        redisRetryAt = System.currentTimeMillis() + REDIS_RETRY_MS;
        LOG.warnf(e, "[Captcha] Redis 不可用，%ds 内降级为进程内校验", REDIS_RETRY_MS / 1000);
    }

    private String render(String code) {
        int width = 130;
        int height = 42;
        StringBuilder sb = new StringBuilder(512);
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
                .append(width).append(' ').append(height).append("\">");
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#f2f4f7\"/>");
        for (int i = 0; i < 4; i++) {
            sb.append("<line x1=\"").append(rand(width)).append("\" y1=\"").append(rand(height))
                    .append("\" x2=\"").append(rand(width)).append("\" y2=\"").append(rand(height))
                    .append("\" stroke=\"").append(randomColor(150, 220)).append("\" stroke-width=\"1\"/>");
        }
        for (int i = 0; i < 20; i++) {
            sb.append("<circle cx=\"").append(rand(width)).append("\" cy=\"").append(rand(height))
                    .append("\" r=\"1\" fill=\"").append(randomColor(120, 200)).append("\"/>");
        }
        int x = 24;
        for (char c : code.toCharArray()) {
            double rotate = (random.nextDouble() - 0.5) * 40; // ±20°
            sb.append("<text x=\"").append(x).append("\" y=\"30\" font-size=\"28\" font-weight=\"bold\"")
                    .append(" font-family=\"Georgia, 'Times New Roman', sans-serif\" fill=\"")
                    .append(randomColor(20, 110))
                    .append("\" transform=\"rotate(").append(String.format("%.1f", rotate))
                    .append(' ').append(x + 8).append(" 22)\">").append(c).append("</text>");
            x += 24;
        }
        sb.append("</svg>");
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private int rand(int bound) {
        return random.nextInt(bound);
    }

    private String randomColor(int min, int max) {
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
