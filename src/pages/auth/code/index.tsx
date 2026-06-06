import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { useAuthStore } from '@/store/useAuthStore';

const CODE_LEN = 6;

const maskPhone = (phone: string) => {
  const p = phone.replace(/\s/g, '');
  if (p.length < 7) return p;
  return `${p.slice(0, 3)} ${p.slice(3, 7)} ${p.slice(7)}`.trim();
};

const LoginCodePage: React.FC = () => {
  const { phone = '', redirect: redirectParam } = Taro.getCurrentInstance().router?.params ?? {};
  const loginWithPhoneMock = useAuthStore((s) => s.loginWithPhoneMock);

  const [code, setCode] = useState('');
  const [leftSec, setLeftSec] = useState(55);
  const inputRef = useRef<any>(null);

  const masked = useMemo(() => maskPhone(decodeURIComponent(phone)), [phone]);
  const canSubmit = code.length === CODE_LEN;
  const redirect = useMemo(() => {
    if (!redirectParam) return '';
    try {
      return decodeURIComponent(redirectParam);
    } catch {
      return String(redirectParam);
    }
  }, [redirectParam]);

  const tabPaths = useMemo(
    () =>
      new Set([
        '/pages/index/index',
        '/pages/services/index',
        '/pages/negatives/index',
        '/pages/orders/index',
        '/pages/mine/index'
      ]),
    []
  );

  useEffect(() => {
    const timer = setInterval(() => {
      setLeftSec((s) => (s <= 0 ? 0 : s - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    setTimeout(() => {
      try {
        inputRef.current?.focus?.();
      } catch (err) {
        console.error('[Input] focus error', err);
      }
    }, 200);
  }, []);

  const goNext = () => {
    const nextUser = useAuthStore.getState().user;
    if (!nextUser?.realName) {
      const url = redirect
        ? `/pages/auth/realName/index?redirect=${encodeURIComponent(redirect)}`
        : '/pages/auth/realName/index';
      Taro.navigateTo({ url }).catch((err) => console.error('[Nav] realName error', err));
      return;
    }
    if (redirect) {
      const [path] = redirect.split('?');
      if (tabPaths.has(path)) {
        Taro.switchTab({ url: path }).catch((err) => console.error('[Nav] switchTab redirect error', err));
        return;
      }
      Taro.redirectTo({ url: redirect }).catch((err) => console.error('[Nav] redirectTo redirect error', err));
      return;
    }
    Taro.switchTab({ url: '/pages/mine/index' }).catch((err) => console.error('[Nav] mine error', err));
  };

  return (
    <View className={styles.container} onClick={() => inputRef.current?.focus?.()}>
      <View
        className={styles.backBtn}
        onClick={() => {
          Taro.navigateBack().catch((err) => console.error('[Nav] back error', err));
        }}
      >
        <Text className={styles.backIcon}>‹</Text>
      </View>

      <View className={styles.brandRow}>
        <View className={styles.brandIcon}>
          <Text className={styles.brandIconText}>📷</Text>
        </View>
        <View className={styles.brandInfo}>
          <Text className={styles.brandName}>琥珀映画</Text>
          <Text className={styles.brandDesc}>记录最美的时光</Text>
        </View>
      </View>

      <Text className={styles.title}>输入验证码</Text>
      <Text className={styles.subtitle}>已发送至 {masked}</Text>

      <View className={styles.codeRow}>
        {Array.from({ length: CODE_LEN }).map((_, idx) => {
          const digit = code[idx] ?? '';
          const isActive = idx === code.length && code.length < CODE_LEN;
          return (
            <View key={idx} className={classnames(styles.codeBox, isActive && styles.codeBoxActive)}>
              <Text className={styles.codeDigit}>{digit}</Text>
            </View>
          );
        })}
      </View>

      <Input
        ref={inputRef}
        className={styles.hiddenInput}
        type='number'
        maxlength={CODE_LEN}
        value={code}
        onInput={(e) => setCode(e.detail.value.slice(0, CODE_LEN))}
      />

      <View className={styles.hintRow}>
        <Text className={styles.hintText}>提示：输入任意6位数字即可（模拟）</Text>
        <Text
          className={styles.resend}
          onClick={() => {
            if (leftSec > 0) return;
            setLeftSec(55);
            Taro.showToast({ title: '已重新发送', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          {leftSec > 0 ? `${leftSec}s 后重新发送` : '重新发送'}
        </Text>
      </View>

      <View
        className={classnames(styles.submitBtn, !canSubmit && styles.submitBtnDisabled)}
        onClick={() => {
          if (!canSubmit) {
            Taro.showToast({ title: '请输入验证码', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
            return;
          }
          loginWithPhoneMock(decodeURIComponent(phone));
          goNext();
        }}
      >
        <Text className={classnames(styles.submitText, !canSubmit && styles.submitTextDisabled)}>验证并登录</Text>
      </View>

      <Text className={styles.footer}>琥珀映画 · 专业摄影服务平台 · 技术支持</Text>
    </View>
  );
};

export default LoginCodePage;
