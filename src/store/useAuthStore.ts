import Taro from '@tarojs/taro';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

export interface AuthUser {
  nickName: string;
  phone?: string;
  realName?: string;
}

interface AuthState {
  isLoggedIn: boolean;
  loginMethod?: 'wechat' | 'phone';
  user?: AuthUser;
  loginWithWechatMock: () => void;
  loginWithPhoneMock: (phone: string) => void;
  setRealName: (realName: string) => void;
  logout: () => void;
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

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      isLoggedIn: false,
      loginMethod: undefined,
      user: undefined,
      loginWithWechatMock: () => {
        const prev = get().user;
        set({
          isLoggedIn: true,
          loginMethod: 'wechat',
          user: {
            nickName: '微信用户',
            phone: undefined,
            realName: prev?.realName
          }
        });
        console.info('[Auth] loginWithWechatMock');
      },
      loginWithPhoneMock: (phone: string) => {
        const prev = get().user;
        set({
          isLoggedIn: true,
          loginMethod: 'phone',
          user: {
            nickName: prev?.nickName || '微信用户',
            phone,
            realName: prev?.realName
          }
        });
        console.info('[Auth] loginWithPhoneMock', { phone });
      },
      setRealName: (realName: string) => {
        const prev = get().user;
        if (!prev) return;
        set({
          user: {
            ...prev,
            realName
          }
        });
        console.info('[Auth] setRealName');
      },
      logout: () => {
        set({ isLoggedIn: false, loginMethod: undefined, user: undefined });
        console.info('[Auth] logout');
      }
    }),
    {
      name: 'auth-store',
      storage: createJSONStorage(() => storage),
      partialize: (state) => ({ isLoggedIn: state.isLoggedIn, loginMethod: state.loginMethod, user: state.user })
    }
  )
);
