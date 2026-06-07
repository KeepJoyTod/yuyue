package com.amberfilm.store;

public record ScheduleSlotDto(String scheduleId, String time, boolean available, int remaining) {
}

