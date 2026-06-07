import { request } from '@/api/request';
import type { BookingCreatedDto, CreateBookingRequestDto, OrderDto } from '@/types/api';
import type { BookingItem, BookingStatus } from '@/types/domain';

const toBookingStatus = (status: string): BookingStatus => {
  if (status === 'confirmed' || status === 'completed' || status === 'cancelled') return status;
  return 'pending';
};

const toBookingItem = (dto: OrderDto): BookingItem => ({
  id: dto.id,
  orderNo: dto.orderNo,
  serviceId: dto.serviceId,
  serviceName: dto.serviceName,
  serviceCoverUrl: dto.serviceCoverUrl,
  storeId: dto.storeId,
  storeName: dto.storeName,
  storeAddress: dto.storeAddress,
  price: dto.price,
  durationMin: dto.durationMin,
  userName: dto.contactName,
  userPhone: dto.contactPhone,
  date: dto.date,
  time: dto.time,
  status: toBookingStatus(dto.status),
  payStatus: dto.payStatus,
  createdAt: dto.createdAt
});

export async function createRemoteBooking(input: CreateBookingRequestDto): Promise<BookingCreatedDto> {
  return request<BookingCreatedDto>('/api/bookings', {
    method: 'POST',
    data: input
  });
}

export async function fetchOrders(status?: BookingStatus): Promise<BookingItem[]> {
  const orders = await request<OrderDto[]>('/api/orders', {
    query: { status }
  });
  return orders.map(toBookingItem);
}

export async function fetchOrderDetail(id: string): Promise<BookingItem> {
  const order = await request<OrderDto>(`/api/orders/${id}`);
  return toBookingItem(order);
}

export async function payOrder(id: string): Promise<BookingItem> {
  const order = await request<OrderDto>(`/api/orders/${id}/pay`, { method: 'POST' });
  return toBookingItem(order);
}

export async function cancelOrder(id: string): Promise<BookingItem> {
  const order = await request<OrderDto>(`/api/orders/${id}/cancel`, { method: 'POST' });
  return toBookingItem(order);
}

export async function completeOrder(id: string): Promise<BookingItem> {
  const order = await request<OrderDto>(`/api/orders/${id}/complete`, { method: 'POST' });
  return toBookingItem(order);
}

export async function deleteOrder(id: string): Promise<void> {
  await request<void>(`/api/orders/${id}`, { method: 'DELETE' });
}
