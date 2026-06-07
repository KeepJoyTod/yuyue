import Taro from '@tarojs/taro';

const TOKEN_KEY = 'amber-auth-token';

export function getAuthToken() {
  try {
    const token = Taro.getStorageSync(TOKEN_KEY);
    return typeof token === 'string' ? token : '';
  } catch (err) {
    console.error('[AuthToken] get error', err);
    return '';
  }
}

export function setAuthToken(token: string) {
  try {
    Taro.setStorageSync(TOKEN_KEY, token);
  } catch (err) {
    console.error('[AuthToken] set error', err);
  }
}

export function clearAuthToken() {
  try {
    Taro.removeStorageSync(TOKEN_KEY);
  } catch (err) {
    console.error('[AuthToken] clear error', err);
  }
}
