package com.amberfilm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:amber-film-admin-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "amber.admin.token=test-admin-token"
})
@AutoConfigureMockMvc
class AdminApiSmokeTest {
  private static final String ADMIN_TOKEN = "test-admin-token";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void missingAdminTokenIsRejected() throws Exception {
    mockMvc.perform(get("/api/admin/summary").header("X-Trace-Id", "trace-admin-auth"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Trace-Id", "trace-admin-auth"))
        .andExpect(jsonPath("$.code").value("ADMIN_AUTH_REQUIRED"));

    mockMvc.perform(get("/api/admin/users/1/assets"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ADMIN_AUTH_REQUIRED"));
  }

  @Test
  void adminReadApisReturnOperationalData() throws Exception {
    JsonNode login = postJson("/api/auth/phone-login", Map.of("phone", "13900139000", "code", "000000"));
    String token = login.path("data").path("token").asText();
    long userId = login.path("data").path("user").path("id").asLong();

    JsonNode uploadToken = postJsonWithToken("/api/files/upload-token", token, Map.of(
        "fileName", "admin-negative.jpg",
        "contentType", "image/jpeg",
        "sizeByte", 1024,
        "usage", "negative"));
    String fileId = uploadToken.path("data").path("fileId").asText();
    assertThat(uploadToken.path("data").path("objectKey").asText()).contains("uploads/users/" + userId);

    JsonNode booking = postJsonWithToken("/api/bookings", token, Map.of(
        "serviceId", "1",
        "storeId", "1",
        "scheduleId", "2",
        "contactName", "李同学",
        "contactPhone", "13900139000"));
    String orderId = booking.path("data").path("id").asText();

    mockMvc.perform(adminGet("/api/admin/users/" + userId + "/assets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.couponCount").value(0))
        .andExpect(jsonPath("$.data.summary.pointBalance").value(0))
        .andExpect(jsonPath("$.data.summary.balanceCent").value(0))
        .andExpect(jsonPath("$.data.summary.cardCount").value(0));

    JsonNode coupon = postJsonWithAdmin("/api/admin/users/" + userId + "/coupons", Map.of(
        "title", "测试优惠券",
        "amountCent", 5000,
        "thresholdCent", 10000,
        "expiresAt", "2026-12-31T23:59:59",
        "reason", "smoke"));
    String couponId = coupon.path("data").path("id").asText();

    postJsonWithAdmin("/api/admin/users/" + userId + "/points/adjust", Map.of(
        "deltaPoints", 120,
        "reason", "smoke"));
    postJsonWithAdmin("/api/admin/users/" + userId + "/wallet/adjust", Map.of(
        "deltaCent", 3000,
        "reason", "smoke"));
    JsonNode card = postJsonWithAdmin("/api/admin/users/" + userId + "/cards", Map.of(
        "title", "测试次卡",
        "totalTimes", 3,
        "expiresAt", "2026-12-31T23:59:59",
        "reason", "smoke"));
    String cardId = card.path("data").path("id").asText();

    mockMvc.perform(get("/api/users/me/summary").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.couponCount").value(1))
        .andExpect(jsonPath("$.data.pointBalance").value(120))
        .andExpect(jsonPath("$.data.balanceCent").value(3000))
        .andExpect(jsonPath("$.data.cardCount").value(1));

    mockMvc.perform(get("/api/users/me/coupons").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(couponId))
        .andExpect(jsonPath("$.data[0].status").value("available"));

    mockMvc.perform(get("/api/users/me/cards").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(cardId))
        .andExpect(jsonPath("$.data[0].remainingTimes").value(3));

    mockMvc.perform(get("/api/users/me/points/transactions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].type").value("admin_adjust"))
        .andExpect(jsonPath("$.data[0].balanceAfter").value(120));

    mockMvc.perform(get("/api/users/me/wallet/transactions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].type").value("admin_adjust"))
        .andExpect(jsonPath("$.data[0].balanceAfterCent").value(3000));

    postJsonWithAdmin("/api/admin/cards/" + cardId + "/adjust-times", Map.of(
        "deltaTimes", -1,
        "reason", "smoke"));
    postJsonWithAdmin("/api/admin/coupons/" + couponId + "/void", Map.of("reason", "smoke"));
    postJsonWithAdmin("/api/admin/cards/" + cardId + "/void", Map.of("reason", "smoke"));

    mockMvc.perform(get("/api/users/me/summary").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.couponCount").value(0))
        .andExpect(jsonPath("$.data.cardCount").value(0));

    JsonNode negative = postJsonWithAdmin("/api/admin/negatives", Map.of(
        "orderId", Long.parseLong(orderId),
        "title", "琥珀 · 精修底片",
        "type", "retouched",
        "fileId", Long.parseLong(fileId),
        "status", "visible"));
    String negativeId = negative.path("data").path("id").asText();

    mockMvc.perform(adminGet("/api/admin/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalUsers").value(1))
        .andExpect(jsonPath("$.data.totalServices").value(3))
        .andExpect(jsonPath("$.data.totalStores").value(2))
        .andExpect(jsonPath("$.data.totalSchedules").value(8))
        .andExpect(jsonPath("$.data.totalOrders").value(1))
        .andExpect(jsonPath("$.data.pendingOrders").value(1))
        .andExpect(jsonPath("$.data.totalNegatives").value(1));

    mockMvc.perform(adminGet("/api/admin/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value("1"))
        .andExpect(jsonPath("$.data[0].categoryName").value("婚纱摄影"));

    mockMvc.perform(adminGet("/api/admin/stores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("琥珀映画·静安旗舰店"));

    mockMvc.perform(adminGet("/api/admin/schedules?date=2026-06-08"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].serviceDate").value("2026-06-08"));

    mockMvc.perform(adminGet("/api/admin/orders?status=pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(orderId))
        .andExpect(jsonPath("$.data[0].userPhone").value("13900139000"))
        .andExpect(jsonPath("$.data[0].contactName").value("李同学"));

    mockMvc.perform(adminGet("/api/admin/negatives?userId=" + userId + "&orderId=" + orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].orderId").value(orderId))
        .andExpect(jsonPath("$.data[0].fileId").value(fileId))
        .andExpect(jsonPath("$.data[0].downloadUrl").exists());

    mockMvc.perform(get("/api/negatives").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(negativeId))
        .andExpect(jsonPath("$.data[0].fileId").value(fileId))
        .andExpect(jsonPath("$.data[0].downloadUrl").exists());

    mockMvc.perform(adminGet("/api/admin/audit-logs?action=NEGATIVE_CREATE&targetType=negative&targetId=" + negativeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].action").value("NEGATIVE_CREATE"))
        .andExpect(jsonPath("$.data[0].targetType").value("negative"))
        .andExpect(jsonPath("$.data[0].targetId").value(negativeId));

    mockMvc.perform(adminGet("/api/admin/audit-logs?action=POINT_ADJUST&targetType=user&targetId=" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].action").value("POINT_ADJUST"));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder adminGet(String path) {
    return get(path).header("X-Admin-Token", ADMIN_TOKEN);
  }

  private JsonNode postJson(String path, Object body) throws Exception {
    MvcResult result = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode postJsonWithToken(String path, String token, Object body) throws Exception {
    MvcResult result = mockMvc.perform(post(path)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode postJsonWithAdmin(String path, Object body) throws Exception {
    MvcResult result = mockMvc.perform(post(path)
            .header("X-Admin-Token", ADMIN_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
