<template>
  <section class="panel orders-page">
    <header class="panel-header">
      <div>
        <span>BOOKING ORDERS</span>
        <h3>预约订单</h3>
      </div>
      <select v-model="statusFilter" @change="loadFilteredOrders">
        <option value="all">全部状态</option>
        <option value="pending">待确认</option>
        <option value="confirmed">已预约</option>
        <option value="completed">已完成</option>
        <option value="cancelled">已取消</option>
      </select>
    </header>

    <div class="orders-table">
      <div class="orders-head">
        <span>订单</span>
        <span>客户</span>
        <span>门店 / 套系</span>
        <span>预约时间</span>
        <span>金额</span>
        <span>状态</span>
        <span>支付</span>
      </div>
      <div v-for="order in adminState.orders" :key="order.id" class="orders-row">
        <span class="mono">{{ order.orderNo }}</span>
        <span>
          <strong>{{ order.contactName }}</strong>
          <small>{{ order.contactPhone || order.userPhone }}</small>
        </span>
        <span>
          <strong>{{ order.storeName }}</strong>
          <small>{{ order.serviceName }}</small>
        </span>
        <span class="mono">{{ order.appointmentAt?.replace('T', ' ').slice(0, 16) }}</span>
        <span>¥{{ (order.priceCent / 100).toLocaleString('zh-CN') }}</span>
        <select :value="order.status" @change="updateStatus(order.id, ($event.target as HTMLSelectElement).value)">
          <option value="pending">待确认</option>
          <option value="confirmed">已预约</option>
          <option value="completed">已完成</option>
          <option value="cancelled">已取消</option>
          <option value="refunded">已退款</option>
        </select>
        <mark>{{ labels.pay(order.payStatus) }}</mark>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { adminApi } from '../api/admin'
import { adminActions, adminState, labels } from '../store'

const statusFilter = ref('all')

const loadFilteredOrders = async () => {
  adminState.orders = await adminApi.orders(statusFilter.value)
}

const updateStatus = async (id: string, status: string) => {
  await adminActions.updateOrder(id, { status })
}
</script>
