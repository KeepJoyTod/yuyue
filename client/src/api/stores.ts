import { request } from '@/api/request';
import type { ScheduleDayDto, StoreDto } from '@/types/api';
import type { ScheduleDay, StoreItem } from '@/types/domain';

const toNumber = (value: number | string | undefined, fallback = 0) => {
  const next = Number(value);
  return Number.isFinite(next) ? next : fallback;
};

const toStore = (dto: StoreDto): StoreItem => ({
  id: dto.id,
  name: dto.name,
  address: dto.address,
  distanceKm: toNumber(dto.distanceKm),
  rating: toNumber(dto.rating),
  reviews: dto.reviews,
  hours: dto.hours,
  tags: dto.tags ?? [],
  coverUrl: dto.coverUrl,
  hasSlotToday: dto.hasSlotToday
});

const toScheduleDay = (dto: ScheduleDayDto): ScheduleDay => ({
  date: dto.date,
  slots: (dto.slots ?? []).map((slot) => ({
    scheduleId: slot.scheduleId,
    time: slot.time,
    available: slot.available,
    remaining: slot.remaining
  }))
});

export async function fetchStores(params: { keyword?: string; tag?: string; serviceId?: string } = {}): Promise<StoreItem[]> {
  const stores = await request<StoreDto[]>('/api/stores', {
    query: {
      ...params,
      tag: params.tag === 'all' ? undefined : params.tag
    }
  });
  return stores.map(toStore);
}

export async function fetchStoreSchedules(params: {
  storeId: string;
  serviceId?: string;
  startDate?: string;
  days?: number;
}): Promise<ScheduleDay[]> {
  const schedules = await request<ScheduleDayDto[]>(`/api/stores/${params.storeId}/schedules`, {
    query: {
      serviceId: params.serviceId,
      startDate: params.startDate,
      days: params.days
    }
  });
  return schedules.map(toScheduleDay);
}
