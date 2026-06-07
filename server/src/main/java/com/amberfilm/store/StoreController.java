package com.amberfilm.store;

import com.amberfilm.common.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {
  private final StoreService storeService;

  public StoreController(StoreService storeService) {
    this.storeService = storeService;
  }

  @GetMapping("/api/stores")
  public ApiResponse<List<StoreDto>> stores(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) Long serviceId) {
    return ApiResponse.ok(storeService.stores(keyword, tag, serviceId));
  }

  @GetMapping("/api/stores/{id}/schedules")
  public ApiResponse<List<ScheduleDayDto>> schedules(
      @PathVariable long id,
      @RequestParam(required = false) Long serviceId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(defaultValue = "7") int days) {
    return ApiResponse.ok(storeService.schedules(id, serviceId, startDate, days));
  }
}

