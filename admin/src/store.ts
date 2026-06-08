import { computed, reactive } from 'vue'
import { adminApi, getAdminToken, setAdminToken } from './api/admin'
import type { AdminNegative, AdminOrder, AdminSchedule, AdminService, AdminStore, AdminSummary, ScheduleLane, StatusCard, TrendDay } from './types'

const emptySummary = (): AdminSummary => ({
  totalUsers: 0,
  totalServices: 0,
  totalStores: 0,
  totalSchedules: 0,
  totalOrders: 0,
  pendingOrders: 0,
  confirmedOrders: 0,
  completedOrders: 0,
  totalNegatives: 0,
  visibleNegatives: 0,
  paidRevenueCent: 0,
})

const pad2 = (value: number) => String(value).padStart(2, '0')
const toDateKey = (date: Date) => `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
const todayKey = () => toDateKey(new Date())

const dateLabel = (date: Date) => `${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
const datePart = (value: string) => value?.slice(0, 10) || ''

const statusLabels: Record<string, string> = {
  pending: '待确认',
  confirmed: '已预约',
  completed: '已完成',
  cancelled: '已取消',
  refunded: '已退款',
}

const payLabels: Record<string, string> = {
  unpaid: '待支付',
  paid: '已支付',
  refunded: '已退款',
}

const scheduleStatusLabels: Record<string, string> = {
  available: '可预约',
  full: '已满',
  closed: '已关闭',
}

export const adminState = reactive({
  loading: false,
  error: '',
  date: todayKey(),
  tokenInput: getAdminToken(),
  summary: emptySummary(),
  services: [] as AdminService[],
  stores: [] as AdminStore[],
  schedules: [] as AdminSchedule[],
  orders: [] as AdminOrder[],
  negatives: [] as AdminNegative[],
})

export const labels = {
  status: (value: string) => statusLabels[value] ?? value,
  pay: (value: string) => payLabels[value] ?? value,
  scheduleStatus: (value: string) => scheduleStatusLabels[value] ?? value,
}

export const adminDerived = {
  revenueText: computed(() => `¥${(adminState.summary.paidRevenueCent / 100).toLocaleString('zh-CN')}`),
  statusCards: computed<StatusCard[]>(() => {
    const confirmed = adminState.summary.confirmedOrders
    const visible = adminState.summary.visibleNegatives
    const retouching = Math.max(0, adminState.summary.totalNegatives - visible)
    return [
      { label: '待确认', value: adminState.summary.pendingOrders, trend: '今日', tone: 'accent' },
      { label: '已到店', value: confirmed, trend: '较昨日', tone: 'dark' },
      { label: '拍摄中', value: confirmed, trend: '工位中', tone: 'dark' },
      { label: '选片中', value: visible, trend: '客片', tone: 'muted' },
      { label: '精修中', value: retouching, trend: '制作', tone: 'muted' },
      { label: '可入册', value: adminState.summary.completedOrders, trend: '交付', tone: 'muted' },
    ]
  }),
  trendDays: computed<TrendDay[]>(() => {
    const days: TrendDay[] = []
    const now = new Date()
    for (let i = 19; i >= 0; i -= 1) {
      const day = new Date(now)
      day.setDate(now.getDate() - i)
      const key = toDateKey(day)
      const rows = adminState.orders.filter(order => datePart(order.appointmentAt || order.createdAt) === key)
      days.push({
        label: dateLabel(day),
        booked: rows.length,
        completed: rows.filter(order => order.status === 'completed' || order.payStatus === 'paid').length,
      })
    }
    return days
  }),
  scheduleLanes: computed<ScheduleLane[]>(() => {
    const map = new Map<string, AdminSchedule[]>()
    for (const schedule of adminState.schedules) {
      const key = schedule.storeName || '未命名门店'
      map.set(key, [...(map.get(key) ?? []), schedule])
    }
    return [...map.entries()].map(([storeName, items]) => ({
      storeName,
      items: items.sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }))
  }),
  recentOrders: computed(() => adminState.orders.slice(0, 8)),
  activeServices: computed(() => adminState.services.filter(service => service.enabled)),
}

export const adminActions = {
  async bootstrap() {
    adminState.loading = true
    adminState.error = ''
    try {
      const [summary, services, stores, schedules, orders, negatives] = await Promise.all([
        adminApi.summary(),
        adminApi.services(),
        adminApi.stores(),
        adminApi.schedules(adminState.date),
        adminApi.orders('all'),
        adminApi.negatives(),
      ])
      adminState.summary = summary
      adminState.services = services
      adminState.stores = stores
      adminState.schedules = schedules
      adminState.orders = orders
      adminState.negatives = negatives
    } catch (error) {
      adminState.error = error instanceof Error ? error.message : '管理后台数据加载失败'
    } finally {
      adminState.loading = false
    }
  },
  async reloadSchedules(date = adminState.date) {
    adminState.date = date
    adminState.schedules = await adminApi.schedules(date)
  },
  async updateOrder(id: string, body: Partial<Pick<AdminOrder, 'status' | 'payStatus' | 'userHidden'>>) {
    const updated = await adminApi.updateOrder(id, body)
    const index = adminState.orders.findIndex(order => order.id === updated.id)
    if (index >= 0) {
      adminState.orders[index] = updated
    }
    adminState.summary = await adminApi.summary()
    return updated
  },
  async saveToken() {
    setAdminToken(adminState.tokenInput)
    await this.bootstrap()
  },
}
