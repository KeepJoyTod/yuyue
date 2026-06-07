package com.amberfilm.catalog;

import java.math.BigDecimal;
import java.util.List;

public record ServiceItemDto(
    String id,
    String name,
    String categoryCode,
    String categoryName,
    String coverUrl,
    int price,
    int priceCent,
    int durationMin,
    String desc,
    List<String> tags,
    BigDecimal rating) {
}

