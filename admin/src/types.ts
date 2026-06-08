export type ApiResponse<T> = {
  code: string
  message: string
  data: T
}

export type AdminSummary = {
  totalUsers: number
  totalServices: number
  totalStores: number
  totalSchedules: number
  totalOrders: number
  pendingOrders: number
  confirmedOrders: number
  completedOrders: number
  totalNegatives: number
  visibleNegatives: number
  paidRevenueCent: number
}

export type AdminService = {
  id: string
  categoryId: string
  categoryName: string
  name: string
  coverUrl: string
  priceCent: number
  durationMin: number
  description: string
  tagsJson: string
  rating: number
  enabled: boolean
  createdAt: string
}

export type AdminStore = {
  id: string
  name: string
  address: string
  distanceKm: number
  rating: number
  reviews: number
  hours: string
  tagsJson: string
  coverUrl: string
  enabled: boolean
  createdAt: string
}

export type AdminSchedule = {
  id: string
  storeId: string
  storeName: string
  serviceId: string
  serviceName: string
  serviceDate: string
  startTime: string
  endTime: string
  capacity: number
  bookedCount: number
  status: string
  createdAt: string
}

export type AdminOrder = {
  id: string
  orderNo: string
  userId: string
  userPhone: string
  serviceId: string
  serviceName: string
  storeId: string
  storeName: string
  scheduleId: string
  contactName: string
  contactPhone: string
  priceCent: number
  appointmentAt: string
  status: string
  payStatus: string
  userHidden: boolean
  createdAt: string
}

export type AdminNegative = {
  id: string
  userId: string
  userPhone: string
  orderId: string
  orderNo: string
  title: string
  type: string
  imageUrl: string
  fileId: string
  downloadUrl: string
  status: string
  createdAt: string
}

export type StatusCard = {
  label: string
  value: number
  trend: string
  tone: 'accent' | 'dark' | 'muted'
}

export type TrendDay = {
  label: string
  booked: number
  completed: number
}

export type ScheduleLane = {
  storeName: string
  items: AdminSchedule[]
}
