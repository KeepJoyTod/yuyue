<template>
  <div class="schedule-board">
    <div class="timeline">
      <span></span>
      <span v-for="hour in hours" :key="hour">{{ hour }}</span>
    </div>
    <div v-if="lanes.length === 0" class="empty-board">暂无今日档期</div>
    <div v-for="lane in lanes" :key="lane.storeName" class="timeline-row">
      <div class="lane-title">{{ lane.storeName }}</div>
      <div class="lane-track">
        <span v-for="hour in hours" :key="hour" class="track-line"></span>
        <article
          v-for="item in lane.items"
          :key="item.id"
          class="booking-chip"
          :class="{ closed: item.status !== 'available' }"
          :style="chipStyle(item)"
        >
          <strong>{{ item.serviceName }}</strong>
          <small>{{ shortTime(item.startTime) }} - {{ shortTime(item.endTime) }}</small>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AdminSchedule, ScheduleLane } from '../types'

defineProps<{ lanes: ScheduleLane[] }>()

const hours = ['09', '10', '11', '12', '13', '14', '15', '16', '17', '18']
const START_HOUR = 9
const END_HOUR = 18

const parseHour = (time: string) => {
  const [hour, minute] = time.split(':').map(Number)
  return (hour || 0) + (minute || 0) / 60
}

const shortTime = (time: string) => time.slice(0, 5)

const chipStyle = (item: AdminSchedule) => {
  const start = Math.max(START_HOUR, parseHour(item.startTime))
  const end = Math.min(END_HOUR, parseHour(item.endTime))
  const span = END_HOUR - START_HOUR
  return {
    left: `${((start - START_HOUR) / span) * 100}%`,
    width: `${Math.max(10, ((end - start) / span) * 100)}%`,
  }
}
</script>
