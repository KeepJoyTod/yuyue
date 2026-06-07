package com.amberfilm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:amber-film-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class AmberFilmApiSmokeTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void userBookingOrderAndNegativeFlowWorks() throws Exception {
    JsonNode login = postJson("/api/auth/phone-login", Map.of("phone", "13800138000", "code", "123456"));
    String token = login.path("data").path("token").asText();
    long userId = login.path("data").path("user").path("id").asLong();

    mockMvc.perform(post("/api/users/real-name")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("realName", "张同学"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.realName").value("张同学"));

    JsonNode booking = postJsonWithToken("/api/bookings", token, Map.of(
        "serviceId", "1",
        "storeId", "1",
        "scheduleId", "1",
        "contactName", "张同学",
        "contactPhone", "13800138000"));
    String orderId = booking.path("data").path("id").asText();

    mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(orderId))
        .andExpect(jsonPath("$.data[0].status").value("pending"));

    mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("confirmed"))
        .andExpect(jsonPath("$.data.payStatus").value("paid"));

    jdbcTemplate.update("""
        INSERT INTO negatives(user_id, order_id, title, type, image_url, status)
        VALUES (?, ?, ?, ?, ?, 'visible')
        """, userId, Long.parseLong(orderId), "梦境 · 白纱系列", "retouched", "https://example.test/negative-1.jpg");

    MvcResult negatives = mockMvc.perform(get("/api/negatives").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].orderId").value(orderId))
        .andExpect(jsonPath("$.data[0].type").value("retouched"))
        .andReturn();

    assertThat(negatives.getResponse().getContentAsString()).contains("negative-1.jpg");
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
}
