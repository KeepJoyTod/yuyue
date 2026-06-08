export type ServiceCategoryId = 'wedding' | 'portrait' | 'kids' | 'business' | 'family';

export interface ServiceCategory {
  id: ServiceCategoryId;
  name: string;
}

export interface ServiceItem {
  id: string;
  name: string;
  categoryId: ServiceCategoryId;
  categoryName?: string;
  coverUrl: string;
  price: number;
  priceCent?: number;
  durationMin: number;
  desc: string;
  tags: string[];
  rating?: number;
}

export interface StoreItem {
  id: string;
  name: string;
  address: string;
  distanceKm: number;
  rating: number;
  reviews: number;
  hours: string;
  tags: string[];
  coverUrl: string;
  hasSlotToday: boolean;
}

export interface ScheduleSlot {
  scheduleId: string;
  time: string;
  available: boolean;
  remaining: number;
}

export interface ScheduleDay {
  date: string;
  slots: ScheduleSlot[];
}

export type BookingStatus = 'pending' | 'confirmed' | 'completed' | 'cancelled';

export interface BookingItem {
  id: string;
  orderNo?: string;
  serviceId: string;
  serviceName: string;
  serviceCoverUrl: string;
  storeId?: string;
  storeName?: string;
  storeAddress?: string;
  price: number;
  durationMin: number;
  userName: string;
  userPhone: string;
  date: string;
  time: string;
  status: BookingStatus;
  payStatus?: string;
  createdAt: string;
}

export interface MemberSummary {
  levelName: string;
  nextLevelName: string;
  growth: number;
  nextNeed: number;
  couponCount: number;
  pointBalance: number;
  balanceCent: number;
  cardCount: number;
  orderCount: number;
  completedOrderCount: number;
}
