<template>
  <article class="stat-card">
    <div class="stat-top">
      <span>{{ item.label }}</span>
      <span :class="['trend-dot', item.tone]">{{ item.trend }}</span>
    </div>
    <strong>{{ item.value }}</strong>
    <small>较昨日</small>
  </article>
</template>

<script setup lang="ts">
import type { StatusCard } from '../types'

defineProps<{ item: StatusCard }>()
</script>
