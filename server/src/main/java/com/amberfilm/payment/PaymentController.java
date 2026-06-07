package com.amberfilm.payment;

import com.amberfilm.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/api/payments/wechat/callback")
  public ApiResponse<PaymentCallbackDto> wechatCallback(@Valid @RequestBody PaymentCallbackRequest request) {
    return ApiResponse.ok(paymentService.handleWechatCallback(request));
  }
}
