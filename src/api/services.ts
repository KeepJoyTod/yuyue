import { request } from '@/api/request';
import type { ServiceCategoryDto, ServiceItemDto } from '@/types/api';
import type { ServiceCategory, ServiceCategoryId, ServiceItem } from '@/types/domain';

const toCategory = (dto: ServiceCategoryDto): ServiceCategory => ({
  id: dto.code as ServiceCategoryId,
  name: dto.name
});

const toServiceItem = (dto: ServiceItemDto): ServiceItem => ({
  id: dto.id,
  name: dto.name,
  categoryId: dto.categoryCode as ServiceCategoryId,
  categoryName: dto.categoryName,
  coverUrl: dto.coverUrl,
  price: dto.price,
  priceCent: dto.priceCent,
  durationMin: dto.durationMin,
  desc: dto.desc,
  tags: dto.tags ?? [],
  rating: typeof dto.rating === 'number' ? dto.rating : Number(dto.rating ?? 0)
});

export async function fetchServiceCategories(): Promise<ServiceCategory[]> {
  const categories = await request<ServiceCategoryDto[]>('/api/service-categories');
  return categories.map(toCategory);
}

export async function fetchServices(params: { keyword?: string; storeId?: string } = {}): Promise<ServiceItem[]> {
  const services = await request<ServiceItemDto[]>('/api/services', { query: params });
  return services.map(toServiceItem);
}

export async function fetchServiceDetail(id: string): Promise<ServiceItem> {
  const service = await request<ServiceItemDto>(`/api/services/${id}`);
  return toServiceItem(service);
}
