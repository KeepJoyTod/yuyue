import type { BookingItem } from '@/types/domain';

export const initialBookings: BookingItem[] = [
  {
    id: 'bk_001',
    serviceId: 'svc_002',
    serviceName: '肩颈放松 · 颈肩专项',
    serviceCoverUrl: 'https://picsum.photos/id/1015/750/500',
    storeId: 'store_jingan',
    storeName: '琥珀映画·静安旗舰店',
    storeAddress: '静安区南京西路 1168 号嘉里中心 3F',
    price: 149,
    durationMin: 45,
    userName: '张同学',
    userPhone: '13800000000',
    date: '2026-06-09',
    time: '19:30',
    status: 'confirmed',
    createdAt: '2026-06-06T10:10:00.000Z'
  },
  {
    id: 'bk_002',
    serviceId: 'svc_004',
    serviceName: '到家保洁 · 基础清洁',
    serviceCoverUrl: 'https://picsum.photos/id/582/750/500',
    storeId: 'store_lujiazui',
    storeName: '琥珀映画·陆家嘴店',
    storeAddress: '浦东新区世纪大道 88 号金茂大厦 L2',
    price: 129,
    durationMin: 120,
    userName: '李女士',
    userPhone: '13900000000',
    date: '2026-06-12',
    time: '10:00',
    status: 'pending',
    createdAt: '2026-06-06T11:30:00.000Z'
  }
];

