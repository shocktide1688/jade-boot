package com.jade.admin;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.eclipse.microprofile.config.ConfigProvider;

import static io.restassured.RestAssured.given;

/**
 * 测试用认证 helper
 * 登录拿 token，给后续请求加 Authorization header
 */
public class AuthHelpers {

    /**
     * 测试端口显式对齐 quarkus.http.test-port: QuarkusTest 对 RestAssured 的端口注入发生在
     * 每个测试方法前, @BeforeAll 阶段发请求时注入尚未发生, given() 会回落 RestAssured 默认
     * 8080 —— test-port 一旦离开 8080 就 Connection refused。@BeforeAll 登录必须显式带端口。
     */
    private static final int TEST_PORT = resolveTestPort();

    private static int resolveTestPort() {
        try {
            return ConfigProvider.getConfig()
                    .getValue("quarkus.http.test-port", Integer.class);
        } catch (Exception e) {
            return RestAssured.port;
        }
    }

    static {
        // 默认 JSON 解析器（response 无 Content-Type 时不报错）
        if (RestAssured.defaultParser == null) {
            RestAssured.defaultParser = Parser.JSON;
        }
    }

    public static String loginAsAdmin() {
        return login("admin", "admin123");
    }

    public static String login(String username, String password) {
        Response r = given()
            .port(TEST_PORT)
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
            .when().post("/api/v1/auth/login");
        if (r.statusCode() == 200 && r.jsonPath().get("data.accessToken") != null) {
            return r.jsonPath().getString("data.accessToken");
        }
        throw new RuntimeException("login failed: " + r.statusCode() + " " + r.body().asString());
    }

    /** 给 RestAssured 设默认 Authorization header (在 QuarkusTest 注入的 requestSpecification 上增强, 不整体替换) */
    public static void authAll(String token) {
        RequestSpecification base =
                RestAssured.requestSpecification != null ? RestAssured.requestSpecification : given();
        RestAssured.requestSpecification = base
            .port(TEST_PORT)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .log().all(false);
    }

    public static void clearAuth() {
        RestAssured.requestSpecification = null;
    }
}
