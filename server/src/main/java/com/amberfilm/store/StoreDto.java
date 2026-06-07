package com.amberfilm.store;

import java.math.BigDecimal;
import java.util.List;

public record StoreDto(
    String id,
    String name,
    String address,
    BigDecimal distanceKm,
    BigDecimal rating,
    int reviews,
    String hours,
    List<String> tags,
    String coverUrl,
    boolean hasSlotToday) {
}

