import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './pages/DashboardView.vue'
import OperationsView from './pages/OperationsView.vue'
import OrdersView from './pages/OrdersView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView, meta: { section: '主控台', title: '预约概况' } },
    { path: '/orders', name: 'orders', component: OrdersView, meta: { section: '主控台', title: '预约订单' } },
    { path: '/schedules', name: 'schedules', component: OperationsView, meta: { section: '主控台', title: '日程管理', resource: 'schedules' } },
    { path: '/stores', name: 'stores', component: OperationsView, meta: { section: '主控台', title: '门店管理', resource: 'stores' } },
    { path: '/services', name: 'services', component: OperationsView, meta: { section: '客片中心', title: '在线选片配置', resource: 'services' } },
    { path: '/negatives', name: 'negatives', component: OperationsView, meta: { section: '客片中心', title: '在线选片', resource: 'negatives' } },
    { path: '/settings', name: 'settings', component: OperationsView, meta: { section: '系统', title: '系统设置', resource: 'settings' } },
  ],
})

export default router
