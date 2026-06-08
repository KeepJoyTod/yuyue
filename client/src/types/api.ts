export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface UserDto {
  id: number;
  nickName: string;
  phone?: string;
  realName?: string;
  avatarUrl?: string;
}

export interface LoginResponseDto {
  token: string;
  user: UserDto;
}

export interface SmsSendResponseDto {
  phone: string;
  scene: string;
  expiresAt: string;
  devCode?: string;
}

export interface UserSummaryDto {
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

export interface ServiceCategoryDto {
  id: string;
  code: string;
  name: string;
}

export interface ServiceItemDto {
  id: string;
  name: string;
  categoryCode: string;
  categoryName: string;
  coverUrl: string;
  price: number;
  priceCent: number;
  durationMin: number;
  desc: string;
  tags: string[];
  rating?: number;
}

export interface StoreDto {
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

export interface ScheduleSlotDto {
  scheduleId: string;
  time: string;
  available: boolean;
  remaining: number;
}

export interface ScheduleDayDto {
  date: string;
  slots: ScheduleSlotDto[];
}

export interface CreateBookingRequestDto {
  serviceId: string;
  storeId: string;
  scheduleId: string;
  contactName: string;
  contactPhone: string;
}

export interface BookingCreatedDto {
  id: string;
  orderNo: string;
  status: string;
  payStatus: string;
}

export interface OrderDto {
  id: string;
  orderNo: string;
  serviceId: string;
  serviceName: string;
  serviceCoverUrl: string;
  storeId: string;
  storeName: string;
  storeAddress: string;
  price: number;
  priceCent: number;
  durationMin: number;
  contactName: string;
  contactPhone: string;
  date: string;
  time: string;
  status: string;
  payStatus: string;
  createdAt: string;
}

export interface NegativeDto {
  id: string;
  orderId: string;
  title: string;
  type: string;
  imageUrl: string;
  fileId?: string;
  downloadUrl?: string;
  createdAt: string;
}
