<template>
  <section v-if="resource === 'schedules'" class="panel">
    <header class="panel-header">
      <div>
        <span>SCHEDULE</span>
        <h3>日程管理</h3>
      </div>
      <input v-model="adminState.date" type="date" @change="adminActions.reloadSchedules(adminState.date)" />
    </header>
    <ScheduleBoard :lanes="adminDerived.scheduleLanes.value" />
  </section>

  <section v-else-if="resource === 'stores'" class="cards-grid">
    <article v-for="store in adminState.stores" :key="store.id" class="data-card">
      <img :src="store.coverUrl" alt="" />
      <div>
        <span>STORE</span>
        <h3>{{ store.name }}</h3>
        <p>{{ store.address }}</p>
      </div>
      <footer>
        <strong>{{ store.rating }}</strong>
        <small>{{ store.hours }}</small>
      </footer>
    </article>
  </section>

  <section v-else-if="resource === 'services'" class="cards-grid">
    <article v-for="service in adminState.services" :key="service.id" class="data-card">
      <img :src="service.coverUrl" alt="" />
      <div>
        <span>{{ service.categoryName }}</span>
        <h3>{{ service.name }}</h3>
        <p>{{ service.description }}</p>
      </div>
      <footer>
        <strong>¥{{ (service.priceCent / 100).toLocaleString('zh-CN') }}</strong>
        <small>{{ service.durationMin }} 分钟</small>
      </footer>
    </article>
  </section>

  <section v-else-if="resource === 'negatives'" class="cards-grid">
    <article v-for="negative in adminState.negatives" :key="negative.id" class="data-card">
      <img :src="negative.imageUrl" alt="" />
      <div>
        <span>{{ negative.type }}</span>
        <h3>{{ negative.title }}</h3>
        <p>{{ negative.orderNo }} · {{ negative.userPhone }}</p>
      </div>
      <footer>
        <strong>{{ negative.status === 'visible' ? '可见' : '隐藏' }}</strong>
        <small>{{ negative.createdAt?.slice(0, 10) }}</small>
      </footer>
    </article>
  </section>

  <section v-else class="panel settings-panel">
    <header class="panel-header">
      <div>
        <span>SETTINGS</span>
        <h3>系统设置</h3>
      </div>
    </header>
    <label class="token-field">
      <span>管理令牌</span>
      <input v-model="adminState.tokenInput" type="password" placeholder="X-Admin-Token" />
      <button type="button" class="primary-button" @click="adminActions.saveToken">保存并刷新</button>
    </label>
    <div class="settings-metrics">
      <article>
        <span>门店</span>
        <strong>{{ adminState.summary.totalStores }}</strong>
      </article>
      <article>
        <span>套系</span>
        <strong>{{ adminState.summary.totalServices }}</strong>
      </article>
      <article>
        <span>订单</span>
        <strong>{{ adminState.summary.totalOrders }}</strong>
      </article>
      <article>
        <span>营收</span>
        <strong>{{ adminDerived.revenueText.value }}</strong>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ScheduleBoard from '../components/ScheduleBoard.vue'
import { adminActions, adminDerived, adminState } from '../store'

const route = useRoute()
const resource = computed(() => route.meta.resource)
</script>
