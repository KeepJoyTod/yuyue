import React, { useEffect } from 'react';
import { useDidShow, useDidHide } from '@tarojs/taro';
import { useAuthStore } from '@/store/useAuthStore';
// 全局样式
import './app.scss';

function App(props) {
  useEffect(() => {
    useAuthStore
      .getState()
      .restoreCurrentUser()
      .catch((err) => {
        console.error('[Auth] restore current user error', err);
        useAuthStore.getState().logout();
      });
  }, []);

  // 可以使用所有的 React Hooks
  useEffect(() => {
    if (process.env.TARO_ENV !== 'h5') return;

    const shouldReload = (msg: string) => {
      const text = (msg || '').toLowerCase();
      return text.includes('loading chunk') || text.includes('chunkloaderror') || text.includes('chunk load');
    };

    const reloadOnce = () => {
      try {
        const key = '__taro_chunk_reload_at__';
        const now = Date.now();
        const last = Number(window.sessionStorage.getItem(key) || '0');
        if (now - last < 10_000) return;
        window.sessionStorage.setItem(key, String(now));
        window.location.reload();
      } catch (err) {
        console.error('[H5] reload on chunk error failed', err);
      }
    };

    const onError = (event: Event) => {
      const anyEvent = event as any;
      const msg = anyEvent?.message || anyEvent?.error?.message || '';
      if (shouldReload(msg)) reloadOnce();
    };

    const onRejection = (event: PromiseRejectionEvent) => {
      const reason: any = event.reason;
      const msg = reason?.message || String(reason || '');
      if (shouldReload(msg)) reloadOnce();
    };

    window.addEventListener('error', onError);
    window.addEventListener('unhandledrejection', onRejection);
    return () => {
      window.removeEventListener('error', onError);
      window.removeEventListener('unhandledrejection', onRejection);
    };
  }, []);

  // 对应 onShow
  useDidShow(() => {});

  // 对应 onHide
  useDidHide(() => {});

  return props.children;
}

export default App;
