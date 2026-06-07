package com.amberfilm.store;

import java.util.List;

public record ScheduleDayDto(String date, List<ScheduleSlotDto> slots) {
}

