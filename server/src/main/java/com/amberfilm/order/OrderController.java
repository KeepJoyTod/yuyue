package com.amberfilm.order;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
  private final AuthService authService;
  private final OrderService orderService;

  public OrderController(AuthService authService, OrderService orderService) {
    this.authService = authService;
    this.orderService = orderService;
  }

  @GetMapping("/api/orders")
  public ApiResponse<List<OrderDto>> list(HttpServletRequest request, @RequestParam(required = false) String status) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(orderService.list(userId, status));
  }

  @GetMapping("/api/orders/{id}")
  public ApiResponse<OrderDto> detail(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(orderService.detail(userId, id));
  }

  @PostMapping("/api/orders/{id}/pay")
  public ApiResponse<OrderDto> pay(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(orderService.pay(userId, id));
  }

  @PostMapping("/api/orders/{id}/cancel")
  public ApiResponse<OrderDto> cancel(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(orderService.cancel(userId, id));
  }

  @PostMapping("/api/orders/{id}/complete")
  public ApiResponse<OrderDto> complete(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(orderService.complete(userId, id));
  }

  @DeleteMapping("/api/orders/{id}")
  public ApiResponse<Void> hide(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    orderService.hide(userId, id);
    return ApiResponse.ok(null);
  }
}

