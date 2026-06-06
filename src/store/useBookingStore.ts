import Taro from '@tarojs/taro';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { initialBookings } from '@/data/bookings';
import type { BookingItem, BookingStatus } from '@/types/domain';
import { createId } from '@/utils/id';

export interface StoreInfo {
  id: string;
  name: string;
  address: string;
}

const defaultStore: StoreInfo = {
  id: 'store_jingan',
  name: '琥珀映画·静安旗舰店',
  address: '静安区南京西路 1168 号嘉里中心 3F'
};

export interface CreateBookingInput {
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
}

interface BookingStoreState {
  hasHydrated: boolean;
  bookings: BookingItem[];
  selectedStore: StoreInfo;
  markHydrated: () => void;
  hydrateDefaults: () => void;
  setSelectedStore: (store: StoreInfo) => void;
  createBooking: (input: CreateBookingInput) => BookingItem;
  updateStatus: (id: string, status: BookingStatus) => void;
  removeBooking: (id: string) => void;
  clearAll: () => void;
}

const storage = {
  getItem: (name: string) => {
    try {
      const value = Taro.getStorageSync(name);
      if (typeof value !== 'string') return null;
      return value;
    } catch (err) {
      console.error('[Storage] getItem error', err);
      return null;
    }
  },
  setItem: (name: string, value: string) => {
    try {
      Taro.setStorageSync(name, value);
    } catch (err) {
      console.error('[Storage] setItem error', err);
    }
  },
  removeItem: (name: string) => {
    try {
      Taro.removeStorageSync(name);
    } catch (err) {
      console.error('[Storage] removeItem error', err);
    }
  }
};

export const useBookingStore = create<BookingStoreState>()(
  persist(
    (set, get) => ({
      hasHydrated: false,
      bookings: [],
      selectedStore: defaultStore,
      markHydrated: () => set({ hasHydrated: true }),
      hydrateDefaults: () => {
        const { bookings, selectedStore } = get();
        if (bookings.length > 0) return;
        set({ bookings: initialBookings });
        if (!selectedStore?.id) set({ selectedStore: defaultStore });
      },
      setSelectedStore: (store) => {
        set({ selectedStore: store });
        console.info('[Booking] setSelectedStore', { id: store.id });
      },
      createBooking: (input) => {
        const selectedStore = get().selectedStore;
        const next: BookingItem = {
          id: createId('bk'),
          ...input,
          storeId: input.storeId ?? selectedStore?.id,
          storeName: input.storeName ?? selectedStore?.name,
          storeAddress: input.storeAddress ?? selectedStore?.address,
          status: 'pending',
          createdAt: new Date().toISOString()
        };

        set((state) => ({ bookings: [next, ...state.bookings] }));
        console.info('[Booking] create', { id: next.id, serviceId: next.serviceId, date: next.date, time: next.time });
        return next;
      },
      updateStatus: (id, status) => {
        set((state) => ({
          bookings: state.bookings.map((b) => (b.id === id ? { ...b, status } : b))
        }));
        console.info('[Booking] updateStatus', { id, status });
      },
      removeBooking: (id) => {
        set((state) => ({ bookings: state.bookings.filter((b) => b.id !== id) }));
        console.info('[Booking] remove', { id });
      },
      clearAll: () => {
        set({ bookings: [] });
        console.info('[Booking] clearAll');
      }
    }),
    {
      name: 'booking-store',
      storage: createJSONStorage(() => storage),
      partialize: (state) => ({ bookings: state.bookings, selectedStore: state.selectedStore }),
      onRehydrateStorage: () => (state, err) => {
        if (err) {
          console.error('[Booking] rehydrate error', err);
        }
        state?.hydrateDefaults();
        if (!state?.selectedStore?.id) {
          state?.setSelectedStore(defaultStore);
        }
        state?.markHydrated();
      }
    }
  )
);
