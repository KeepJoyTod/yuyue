import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { useAuthStore } from '@/store/useAuthStore';

const RealNamePage: React.FC = () => {
  const { redirect: redirectParam } = Taro.getCurrentInstance().router?.params ?? {};
  const user = useAuthStore((s) => s.user);
  const loginMethod = useAuthStore((s) => s.loginMethod);
  const saveRealName = useAuthStore((s) => s.saveRealName);

  const [realName, setRealNameInput] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
    const { isLoggedIn } = useAuthStore.getState();
    if (isLoggedIn) return;
    const nextRedirect = redirect
      ? `/pages/auth/realName/index?redirect=${encodeURIComponent(redirect)}`
      : '/pages/auth/realName/index';
    Taro.redirectTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent(nextRedirect)}` }).catch((err) =>
      console.error('[Nav] redirectTo login error', err)
    );
    Taro.showToast({ title: '请先登录', icon: 'none' }).catch((err) => console.error('[Toast] error', err));
  }, [redirect]);

  const phoneText = useMemo(() => {
    if (loginMethod !== 'phone') return '';
    const p = user?.phone ?? '';
    return p.replace(/\s/g, '');
  }, [loginMethod, user?.phone]);

  const canSubmit = realName.trim().length >= 2 && !submitting;

  return (
    <View className={styles.container}>
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

      <Text className={styles.title}>完善资料</Text>
      <Text className={styles.subtitle}>新用户注册，完成后立即获得 200 积分</Text>

      <Text className={styles.label}>您的姓名</Text>
      <Input
        className={styles.input}
        placeholder='请输入真实姓名'
        value={realName}
        onInput={(e) => setRealNameInput(e.detail.value)}
      />

      <View className={styles.noteCard}>
        <View className={styles.noteIcon}>
          <Text className={styles.noteIconText}>🛡</Text>
        </View>
        {!!phoneText ? (
          <Text className={styles.noteText}>您的手机号 {phoneText} 将作为账号登录凭证，姓名仅用于预约确认，不对外公开。</Text>
        ) : (
          <Text className={styles.noteText}>您的账号将作为登录凭证，姓名仅用于预约确认，不对外公开。</Text>
        )}
      </View>

      <View
        className={classnames(styles.submitBtn, !canSubmit && styles.submitBtnDisabled)}
        onClick={() => {
          if (!canSubmit) {
            Taro.showToast({ title: '请输入真实姓名', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
            return;
          }
          setSubmitting(true);
          saveRealName(realName.trim())
            .then(() => {
              if (redirect) {
                const [path] = redirect.split('?');
                if (tabPaths.has(path)) {
                  Taro.switchTab({ url: path }).catch((err) => console.error('[Nav] switchTab redirect error', err));
                } else {
                  Taro.redirectTo({ url: redirect }).catch((err) => console.error('[Nav] redirectTo redirect error', err));
                }
              } else {
                Taro.switchTab({ url: '/pages/mine/index' }).catch((err) => console.error('[Nav] mine error', err));
              }
              Taro.showToast({ title: '登录成功', icon: 'success' }).catch((err) => console.error('[Toast] error', err));
            })
            .catch((err) => {
              console.error('[Auth] save real name error', err);
              Taro.showToast({ title: '保存失败，请重试', icon: 'none' }).catch((toastErr) =>
                console.error('[Toast] showToast error', toastErr)
              );
            })
            .finally(() => setSubmitting(false));
        }}
      >
        <Text className={classnames(styles.submitText, !canSubmit && styles.submitTextDisabled)}>
          {submitting ? '提交中...' : '立即注册并登录'}
        </Text>
      </View>

      <Text className={styles.footer}>琥珀映画 · 专业摄影服务平台 · 技术支持</Text>
    </View>
  );
};

export default RealNamePage;
