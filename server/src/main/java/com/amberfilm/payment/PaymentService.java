package com.amberfilm.payment;

import com.amberfilm.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private final JdbcTemplate jdbcTemplate;
  private final boolean devCallbackEnabled;
  private final String callbackSecret;

  public PaymentService(
      JdbcTemplate jdbcTemplate,
      @Value("${amber.payment.dev-callback-enabled:true}") boolean devCallbackEnabled,
      @Value("${amber.payment.callback-secret:${amber.auth.token-secret}}") String callbackSecret) {
    this.jdbcTemplate = jdbcTemplate;
    this.devCallbackEnabled = devCallbackEnabled;
    this.callbackSecret = callbackSecret;
  }

  @Transactional
  public PaymentCallbackDto handleWechatCallback(PaymentCallbackRequest request) {
    verifySignature(request);
    Map<String, Object> order = requireOrderByNo(request.orderNo());
    long orderId = ((Number) order.get("id")).longValue();
    try {
      jdbcTemplate.update("""
          INSERT INTO payment_events(order_id, channel, event_type, transaction_no, raw_payload)
          VALUES (?, 'wechat', 'paid', ?, ?)
          """, orderId, request.transactionNo(), request.rawPayload());
    } catch (DuplicateKeyException ex) {
      return new PaymentCallbackDto(String.valueOf(orderId), request.orderNo(), (String) order.get("status"), true);
    }
    if (((Number) order.get("price_cent")).intValue() != request.amountCent()) {
      throw ApiException.badRequest("PAYMENT_AMOUNT_MISMATCH", "支付金额与订单金额不一致");
    }
    if (!"paid".equals(order.get("pay_status"))) {
      jdbcTemplate.update("""
          UPDATE orders
          SET status = 'confirmed', pay_status = 'paid', updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
          """, orderId);
      jdbcTemplate.update("""
          INSERT INTO payments(order_id, channel, amount_cent, status, transaction_no, paid_at)
          VALUES (?, 'wechat', ?, 'success', ?, CURRENT_TIMESTAMP)
          """, orderId, request.amountCent(), request.transactionNo());
    }
    return new PaymentCallbackDto(String.valueOf(orderId), request.orderNo(), "confirmed", false);
  }

  private Map<String, Object> requireOrderByNo(String orderNo) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, order_no, price_cent, status, pay_status
        FROM orders
        WHERE order_no = ?
        FOR UPDATE
        """, orderNo);
    if (rows.isEmpty()) {
      throw ApiException.notFound("订单不存在");
    }
    return rows.get(0);
  }

  private void verifySignature(PaymentCallbackRequest request) {
    if (devCallbackEnabled && (request.signature() == null || request.signature().isBlank())) {
      return;
    }
    String expected = sign(request.orderNo() + ":" + request.transactionNo() + ":" + request.amountCent());
    if (request.signature() == null
        || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8))) {
      throw ApiException.badRequest("PAYMENT_SIGNATURE_INVALID", "支付回调签名无效");
    }
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Payment callback signing failed", ex);
    }
  }
}
