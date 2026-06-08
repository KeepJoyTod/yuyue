import Taro from '@tarojs/taro';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

export interface StoreInfo {
  id: string;
  name: string;
  address: string;
}

const defaultStore: StoreInfo = {
  id: '1',
  name: '琥珀映画·静安旗舰店',
  address: '静安区南京西路 1168 号嘉里中心 3F'
};

interface BookingStoreState {
  hasHydrated: boolean;
  selectedStore: StoreInfo;
  markHydrated: () => void;
  setSelectedStore: (store: StoreInfo) => void;
}

const storage = {
  getItem: (name: string) => {
    try {
      const value = Taro.getStorageSync(name);
      if (typeof value !== 'string') return null;
      if (!value.trim()) {
        Taro.removeStorageSync(name);
        return null;
      }
      try {
        JSON.parse(value);
      } catch {
        Taro.removeStorageSync(name);
        return null;
      }
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
    (set) => ({
      hasHydrated: false,
      selectedStore: defaultStore,
      markHydrated: () => set({ hasHydrated: true }),
      setSelectedStore: (store) => {
        set({ selectedStore: store });
        console.info('[Booking] setSelectedStore', { id: store.id });
      }
    }),
    {
      name: 'booking-store',
      storage: createJSONStorage(() => storage),
      partialize: (state) => ({ selectedStore: state.selectedStore }),
      onRehydrateStorage: () => (state, err) => {
        if (err) {
          console.error('[Booking] rehydrate error', err);
        }
        if (!state?.selectedStore?.id) {
          state?.setSelectedStore(defaultStore);
        }
        state?.markHydrated();
      }
    }
  )
);
