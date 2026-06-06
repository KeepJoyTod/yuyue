import type { ServiceCategory, ServiceItem } from '@/types/domain';

export const serviceCategories: ServiceCategory[] = [
  { id: 'wedding', name: '婚纱摄影' },
  { id: 'portrait', name: '写真套系' },
  { id: 'kids', name: '儿童摄影' },
  { id: 'business', name: '商务形象' },
  { id: 'family', name: '全家福' }
];

export const serviceList: ServiceItem[] = [
  {
    id: 'pkg_001',
    name: '梦境 · 白纱系列',
    categoryId: 'wedding',
    coverUrl: 'https://picsum.photos/id/91/750/500',
    price: 4980,
    durationMin: 180,
    desc: '经典白纱 + 轻法式氛围，含造型与场景搭配建议，适合新人首选。',
    tags: ['婚纱摄影', '白纱', '高口碑'],
    rating: 4.9
  },
  {
    id: 'pkg_002',
    name: '琥珀 · 轻奢写真',
    categoryId: 'portrait',
    coverUrl: 'https://picsum.photos/id/64/750/500',
    price: 2280,
    durationMin: 120,
    desc: '轻奢质感棚拍路线，妆发更精致，适合日常纪念与个人写真。',
    tags: ['写真套系', '轻奢', '氛围感'],
    rating: 4.8
  },
  {
    id: 'pkg_003',
    name: '亲子时光',
    categoryId: 'kids',
    coverUrl: 'https://picsum.photos/id/1025/750/500',
    price: 1980,
    durationMin: 120,
    desc: '亲子互动引导更自然，记录温暖瞬间，适合家庭日常纪念。',
    tags: ['儿童摄影', '亲子', '温馨'],
    rating: 4.9
  }
];
