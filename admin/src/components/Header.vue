<template>
  <header class="topbar">
    <div>
      <div class="crumb">{{ route.meta.section }} / {{ route.meta.title }}</div>
      <h1>{{ route.meta.title }}</h1>
    </div>

    <div class="topbar-actions">
      <label class="search-box">
        <Search :size="16" />
        <input v-model="searchValue" type="search" placeholder="搜索客户姓名 / 订单号 / 手机号 / 店片" />
        <kbd>⌘K</kbd>
      </label>
      <button class="icon-button" type="button" aria-label="通知">
        <Bell :size="17" />
      </button>
      <button class="primary-button" type="button" @click="$emit('newAppointment')">
        <Plus :size="17" />
        <span>新建预约</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, Plus, Search } from 'lucide-vue-next'

defineEmits<{ newAppointment: [] }>()

const route = useRoute()
const searchValue = ref('')
</script>
