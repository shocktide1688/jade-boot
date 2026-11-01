package com.jade.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 不可用时的登录保护降级验证
 *
 * 指向一个无服务的 Redis 端口（连接拒绝、快速失败）：
 * 失败计数与验证码应自动降级为进程内存储，超次强制验证码全流程仍可用，
 * 且校验不再 fail-open（错答案必须被拒绝）。
 */
@QuarkusTest
@TestProfile(NoRedisLoginAttemptTest.NoRedisProfile.class)
class NoRedisLoginAttemptTest {

    public static class NoRedisProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.redis.hosts", "redis://localhost:6399");
        }
    }

    /** 从我们生成的 SVG（base64 data URI）里按顺序抠出验证码字符 */
    private String solveCaptcha(String dataUri) {
        String svg = new String(Base64.getDecoder().decode(
                dataUri.substring("data:image/svg+xml;base64,".length())), StandardCharsets.UTF_8);
        StringBuilder code = new StringBuilder();
        Matcher m = Pattern.compile("<text[^>]*>([^<]+)</text>").matcher(svg);
        while (m.find()) {
            code.append(m.group(1));
        }
        assertEquals(4, code.length(), "SVG 中应包含 4 个验证码字符: " + svg);
        return code.toString();
    }

    @Test
    void captcha_flow_works_without_redis() {
        // 1) 连错 5 次（阈值 5）——计数走进程内降级存储
        String wrong = "{\"username\":\"admin\",\"password\":\"wrong\"}";
        for (int i = 0; i < 5; i++) {
            given().contentType(ContentType.JSON).body(wrong)
                    .when().post("/api/v1/auth/login")
                    .then().statusCode(200).body("code", equalTo(1004));
        }

        // 2) 达到阈值：正确密码也必须带验证码
        given().contentType(ContentType.JSON).body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200).body("code", equalTo(1005));

        // 3) 拉验证码（答案存进程内）+ 填错答案 → 必须拒绝（不放行）
        var captcha = given().when().get("/api/v1/auth/captcha")
                .then().statusCode(200).body("code", equalTo(0))
                .extract().jsonPath();
        String uuid = captcha.getString("data.uuid");
        String code = solveCaptcha(captcha.getString("data.image"));

        given().contentType(ContentType.JSON).body(
                        "{\"username\":\"admin\",\"password\":\"admin123\",\"uuid\":\"" + uuid + "\",\"code\":\"XXXX\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200).body("code", equalTo(1006));

        // 4) uuid 一次性已作废；重新拉一张、用正确答案 → 登录成功
        var fresh = given().when().get("/api/v1/auth/captcha")
                .then().statusCode(200).body("code", equalTo(0))
                .extract().jsonPath();
        String freshUuid = fresh.getString("data.uuid");
        String freshCode = solveCaptcha(fresh.getString("data.image"));
        assertTrue(freshCode.matches("[A-Z0-9]{4}"));

        given().contentType(ContentType.JSON).body(
                        "{\"username\":\"admin\",\"password\":\"admin123\",\"uuid\":\"" + freshUuid + "\",\"code\":\"" + freshCode + "\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200).body("code", equalTo(0));

        // 5) 成功后计数重置：不再要求验证码
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200).body("code", equalTo(0));
    }
}
