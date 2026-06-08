import type {
  AdminNegative,
  AdminOrder,
  AdminSchedule,
  AdminService,
  AdminStore,
  AdminSummary,
  ApiResponse,
} from '../types'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''
const ADMIN_TOKEN_KEY = 'amber-film-admin-token'
const DEFAULT_ADMIN_TOKEN = import.meta.env.VITE_ADMIN_TOKEN ?? 'dev-admin-token'

export const getAdminToken = () => localStorage.getItem(ADMIN_TOKEN_KEY) || DEFAULT_ADMIN_TOKEN

export const setAdminToken = (token: string) => {
  const trimmed = token.trim()
  if (trimmed) {
    localStorage.setItem(ADMIN_TOKEN_KEY, trimmed)
  } else {
    localStorage.removeItem(ADMIN_TOKEN_KEY)
  }
}

const buildUrl = (path: string, query?: Record<string, string | number | boolean | null | undefined>) => {
  const url = new URL(`${API_BASE}${path}`, window.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value === null || value === undefined || value === '') continue
    url.searchParams.set(key, String(value))
  }
  return API_BASE ? url.toString() : `${url.pathname}${url.search}`
}

const request = async <T>(
  path: string,
  init: RequestInit = {},
  query?: Record<string, string | number | boolean | null | undefined>,
) => {
  const headers = new Headers(init.headers)
  const hasBody = init.body !== undefined && init.body !== null
  if (hasBody && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  headers.set('X-Admin-Token', getAdminToken())

  const response = await fetch(buildUrl(path, query), { ...init, headers })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const json = (await response.json()) as ApiResponse<T>
  if (json.code !== 'OK') {
    throw new Error(json.message || json.code || 'Request failed')
  }
  return json.data
}

export const adminApi = {
  summary: () => request<AdminSummary>('/api/admin/summary'),
  services: () => request<AdminService[]>('/api/admin/services'),
  stores: () => request<AdminStore[]>('/api/admin/stores'),
  schedules: (date?: string) => request<AdminSchedule[]>('/api/admin/schedules', {}, { date }),
  orders: (status?: string) => request<AdminOrder[]>('/api/admin/orders', {}, { status }),
  negatives: () => request<AdminNegative[]>('/api/admin/negatives'),
  updateOrder: (id: string, body: Partial<Pick<AdminOrder, 'status' | 'payStatus' | 'userHidden'>>) =>
    request<AdminOrder>(`/api/admin/orders/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
}
