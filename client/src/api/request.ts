import Taro from '@tarojs/taro';
import { clearAuthToken, getAuthToken } from '@/api/token';
import type { ApiResponse } from '@/types/api';

type QueryValue = string | number | boolean | undefined | null;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  data?: unknown;
  query?: Record<string, QueryValue>;
  showErrorToast?: boolean;
}

const API_BASE_URL = __API_BASE_URL__.replace(/\/$/, '');

const buildUrl = (path: string, query?: Record<string, QueryValue>) => {
  const url = `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
  const params = Object.entries(query ?? {}).filter(([, value]) => value !== undefined && value !== null && value !== '');
  if (params.length === 0) return url;
  const search = params
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&');
  return `${url}?${search}`;
};

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', data, query, showErrorToast = true } = options;

  try {
    const token = getAuthToken();
    const result = await Taro.request<ApiResponse<T>>({
      url: buildUrl(path, query),
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    });

    const body = result.data;
    if (result.statusCode < 200 || result.statusCode >= 300 || body?.code !== 'OK') {
      const message = body?.message || `请求失败(${result.statusCode})`;
      if (result.statusCode === 401 || body?.code === 'AUTH_REQUIRED') {
        clearAuthToken();
      }
      throw new Error(message);
    }

    return body.data;
  } catch (err) {
    const message = err instanceof Error ? err.message : '网络请求失败';
    if (showErrorToast) {
      Taro.showToast({ title: message, icon: 'none' }).catch((toastErr) =>
        console.error('[Toast] request error toast failed', toastErr)
      );
    }
    throw err;
  }
}
