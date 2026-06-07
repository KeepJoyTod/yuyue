package com.amberfilm.booking;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {
  private final AuthService authService;
  private final BookingService bookingService;

  public BookingController(AuthService authService, BookingService bookingService) {
    this.authService = authService;
    this.bookingService = bookingService;
  }

  @PostMapping("/api/bookings")
  public ApiResponse<BookingCreatedDto> create(HttpServletRequest request, @Valid @RequestBody CreateBookingRequest body) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(bookingService.create(userId, body));
  }
}

