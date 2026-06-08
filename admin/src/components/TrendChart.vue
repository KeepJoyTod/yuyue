<template>
  <div class="trend-chart">
    <svg viewBox="0 0 680 300" role="img" aria-label="预约趋势">
      <g class="grid-lines">
        <line v-for="line in grid" :key="line" x1="36" x2="660" :y1="line" :y2="line" />
      </g>
      <polyline class="line booked" :points="bookedPoints" />
      <polyline class="line completed" :points="completedPoints" />
      <g class="axis-labels">
        <text v-for="label in visibleLabels" :key="label.x" :x="label.x" y="286">{{ label.text }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TrendDay } from '../types'

const props = defineProps<{ days: TrendDay[] }>()

const grid = [42, 98, 154, 210, 266]

const maxValue = computed(() => Math.max(1, ...props.days.flatMap(day => [day.booked, day.completed])))

const toPoints = (key: 'booked' | 'completed') => {
  const width = 624
  const height = 224
  const count = Math.max(1, props.days.length - 1)
  return props.days
    .map((day, index) => {
      const x = 36 + (width * index) / count
      const y = 266 - (height * day[key]) / maxValue.value
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

const bookedPoints = computed(() => toPoints('booked'))
const completedPoints = computed(() => toPoints('completed'))

const visibleLabels = computed(() => {
  const count = Math.max(1, props.days.length - 1)
  return props.days
    .filter((_, index) => index % 3 === 0 || index === props.days.length - 1)
    .map(day => {
      const index = props.days.indexOf(day)
      return {
        text: day.label,
        x: 36 + (624 * index) / count,
      }
    })
})
</script>
