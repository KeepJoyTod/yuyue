import Taro from '@tarojs/taro';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { fetchCurrentUser, loginByPhone, updateRealName } from '@/api/auth';
import { clearAuthToken, setAuthToken } from '@/api/token';

export interface AuthUser {
  id?: string;
  nickName: string;
  phone?: string;
  realName?: string;
  avatarUrl?: string;
}

interface AuthState {
  isLoggedIn: boolean;
  loginMethod?: 'wechat' | 'phone';
  token?: string;
  user?: AuthUser;
  loginWithPhone: (phone: string, code: string) => Promise<void>;
  restoreCurrentUser: () => Promise<void>;
  saveRealName: (realName: string) => Promise<void>;
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
      token: undefined,
      user: undefined,
      loginWithPhone: async (phone: string, code: string) => {
        const result = await loginByPhone(phone, code);
        setAuthToken(result.token);
        set({
          isLoggedIn: true,
          loginMethod: 'phone',
          token: result.token,
          user: result.user
        });
        console.info('[Auth] loginWithPhone', { phone });
      },
      restoreCurrentUser: async () => {
        const token = get().token;
        if (!token) return;
        setAuthToken(token);
        const user = await fetchCurrentUser();
        set({
          isLoggedIn: true,
          user
        });
        console.info('[Auth] restoreCurrentUser');
      },
      saveRealName: async (realName: string) => {
        const user = await updateRealName(realName);
        set({
          isLoggedIn: true,
          user
        });
        console.info('[Auth] saveRealName');
      },
      logout: () => {
        clearAuthToken();
        set({ isLoggedIn: false, loginMethod: undefined, token: undefined, user: undefined });
        console.info('[Auth] logout');
      }
    }),
    {
      name: 'auth-store',
      storage: createJSONStorage(() => storage),
      partialize: (state) => ({ isLoggedIn: state.isLoggedIn, loginMethod: state.loginMethod, token: state.token, user: state.user }),
      onRehydrateStorage: () => (state) => {
        if (state?.token) setAuthToken(state.token);
      }
    }
  )
);
