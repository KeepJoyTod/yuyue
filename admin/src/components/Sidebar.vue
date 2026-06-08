<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">
        <Aperture :size="22" />
      </div>
      <div>
        <strong>琥珀映画</strong>
        <span>AMBER STUDIO · CMS</span>
      </div>
    </div>

    <nav class="nav">
      <p>主控台</p>
      <RouterLink v-for="item in primaryItems" :key="item.path" :to="item.path" class="nav-item">
        <component :is="item.icon" :size="18" />
        <span>{{ item.label }}</span>
      </RouterLink>

      <p>客片中心</p>
      <RouterLink v-for="item in albumItems" :key="item.path" :to="item.path" class="nav-item">
        <component :is="item.icon" :size="18" />
        <span>{{ item.label }}</span>
      </RouterLink>

      <p>系统</p>
      <RouterLink to="/settings" class="nav-item">
        <Settings :size="18" />
        <span>系统设置</span>
      </RouterLink>
    </nav>

    <div class="operator">
      <div class="avatar">沈</div>
      <div>
        <strong>沈知秋</strong>
        <span>门店主管</span>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Aperture, CalendarDays, ClipboardList, GalleryHorizontalEnd, LayoutDashboard, Settings, SlidersHorizontal, Store, Images } from 'lucide-vue-next'

const primaryItems = [
  { path: '/', label: '预约概况', icon: LayoutDashboard },
  { path: '/orders', label: '预约订单', icon: ClipboardList },
  { path: '/schedules', label: '日程管理', icon: CalendarDays },
  { path: '/stores', label: '门店管理', icon: Store },
]

const albumItems = [
  { path: '/services', label: '在线选片配置', icon: SlidersHorizontal },
  { path: '/negatives', label: '在线选片', icon: Images },
  { path: '/negatives', label: '客片管理', icon: GalleryHorizontalEnd },
]
</script>
