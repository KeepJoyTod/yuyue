package com.amberfilm.catalog;

import com.amberfilm.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {
  private final CatalogService catalogService;

  public CatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/api/service-categories")
  public ApiResponse<List<CategoryDto>> categories() {
    return ApiResponse.ok(catalogService.categories());
  }

  @GetMapping("/api/services")
  public ApiResponse<List<ServiceItemDto>> services(
      @RequestParam(required = false) String categoryCode,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long storeId) {
    return ApiResponse.ok(catalogService.services(categoryCode, keyword, storeId));
  }

  @GetMapping("/api/services/{id}")
  public ApiResponse<ServiceItemDto> serviceDetail(@PathVariable long id) {
    return ApiResponse.ok(catalogService.serviceDetail(id));
  }
}

