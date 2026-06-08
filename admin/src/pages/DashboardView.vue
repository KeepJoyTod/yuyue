<template>
  <div class="page-stack">
    <section class="section-block">
      <div class="section-title-row">
        <div>
          <h2>服务订单状态</h2>
          <p>今日 {{ todayText }} · 全门店汇总</p>
        </div>
        <button type="button" class="ghost-action">导出日报 ↗</button>
      </div>
      <div class="status-strip">
        <StatCard v-for="item in adminDerived.statusCards.value" :key="item.label" :item="item" />
      </div>
    </section>

    <div class="dashboard-grid">
      <section class="panel">
        <header class="panel-header">
          <div>
            <span>BOOKING TREND</span>
            <h3>预约趋势 · 近 20 日</h3>
          </div>
          <div class="legend">
            <i class="accent"></i>预约
            <i></i>到店
            <button type="button">月 / 周 / 日</button>
          </div>
        </header>
        <TrendChart :days="adminDerived.trendDays.value" />
      </section>

      <section class="panel">
        <header class="panel-header">
          <div>
            <span>TODAY · 工位时段</span>
            <h3>预约时段工位</h3>
          </div>
          <input v-model="adminState.date" type="date" @change="adminActions.reloadSchedules(adminState.date)" />
        </header>
        <ScheduleBoard :lanes="adminDerived.scheduleLanes.value" />
      </section>
    </div>

    <section class="panel">
      <header class="panel-header">
        <div>
          <span>RECENT ORDERS</span>
          <h3>最新预约订单</h3>
        </div>
        <RouterLink to="/orders" class="ghost-action">查看全部</RouterLink>
      </header>
      <div class="mini-table">
        <div class="mini-head">
          <span>订单号</span>
          <span>客户</span>
          <span>套系</span>
          <span>预约时间</span>
          <span>状态</span>
        </div>
        <div v-for="order in adminDerived.recentOrders.value" :key="order.id" class="mini-row">
          <span>{{ order.orderNo }}</span>
          <strong>{{ order.contactName }}</strong>
          <span>{{ order.serviceName }}</span>
          <span>{{ order.appointmentAt?.slice(5, 16) }}</span>
          <mark>{{ labels.status(order.status) }}</mark>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import ScheduleBoard from '../components/ScheduleBoard.vue'
import StatCard from '../components/StatCard.vue'
import TrendChart from '../components/TrendChart.vue'
import { adminActions, adminDerived, adminState, labels } from '../store'

const now = new Date()
const todayText = `${now.getMonth() + 1} 月 ${now.getDate()} 日`
</script>
