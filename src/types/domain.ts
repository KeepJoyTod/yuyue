export type ServiceCategoryId = 'wedding' | 'portrait' | 'kids' | 'business' | 'family';

export interface ServiceCategory {
  id: ServiceCategoryId;
  name: string;
}

export interface ServiceItem {
  id: string;
  name: string;
  categoryId: ServiceCategoryId;
  coverUrl: string;
  price: number;
  durationMin: number;
  desc: string;
  tags: string[];
  rating?: number;
}

export type BookingStatus = 'pending' | 'confirmed' | 'completed' | 'cancelled';

export interface BookingItem {
  id: string;
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
  createdAt: string;
}
