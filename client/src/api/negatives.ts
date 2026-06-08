import { request } from '@/api/request';
import type { NegativeDto } from '@/types/api';

export interface NegativeSession {
  id: string;
  coverUrl: string;
  name: string;
  shootDate: string;
  totalCount: number;
  selectedCount?: number;
  refinedCount?: number;
  status: 'pendingSelect' | 'pendingSubmit' | 'completed';
  photos: { id: string; src: string }[];
}

export async function fetchNegativeSessions(): Promise<NegativeSession[]> {
  const negatives = await request<NegativeDto[]>('/api/negatives');
  const groups = new Map<string, NegativeDto[]>();

  negatives.forEach((item) => {
    const items = groups.get(item.orderId) ?? [];
    items.push(item);
    groups.set(item.orderId, items);
  });

  return Array.from(groups.entries()).map(([orderId, items]) => {
    const sorted = [...items].sort((a, b) => Date.parse(a.createdAt) - Date.parse(b.createdAt));
    const retouchedCount = sorted.filter((item) => item.type === 'retouched').length;
    const first = sorted[0];
    const totalCount = sorted.length;
    const refinedCount = retouchedCount || totalCount;

    return {
      id: orderId,
      coverUrl: first?.downloadUrl || first?.imageUrl || '',
      name: first?.title ?? '摄影底片',
      shootDate: first?.createdAt?.slice(0, 10) ?? '',
      totalCount,
      selectedCount: retouchedCount,
      refinedCount,
      status: retouchedCount > 0 ? 'completed' : 'pendingSelect',
      photos: sorted.map((item) => ({ id: item.id, src: item.downloadUrl || item.imageUrl }))
    };
  });
}
