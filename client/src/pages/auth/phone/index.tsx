import React, { useMemo, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { sendLoginSms } from '@/api/auth';

const LoginPhonePage: React.FC = () => {
  const { redirect: redirectParam } = Taro.getCurrentInstance().router?.params ?? {};
  const [phone, setPhone] = useState('');
  const [sending, setSending] = useState(false);

  const cleanPhone = useMemo(() => phone.replace(/\s/g, ''), [phone]);
  const canNext = /^1\d{10}$/.test(cleanPhone) && !sending;
  const redirect = useMemo(() => {
    if (!redirectParam) return '';
    try {
      return decodeURIComponent(redirectParam);
    } catch {
      return String(redirectParam);
    }
  }, [redirectParam]);

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

      <Text className={styles.title}>输入手机号</Text>
      <Text className={styles.subtitle}>未注册的手机号将自动创建账号</Text>

      <View className={styles.inputRow}>
        <Text className={styles.countryCode}>+86</Text>
        <View className={styles.divider} />
        <Input
          className={styles.input}
          type='number'
          placeholder='请输入手机号'
          value={phone}
          onInput={(e) => setPhone(e.detail.value)}
        />
        <View className={styles.eye}>
          <Text className={styles.eyeText}>👁</Text>
        </View>
      </View>

      <Text className={styles.hint}>
        提示：验证码由后端发送，开发环境会直接显示本次验证码。
      </Text>

      <View
        className={classnames(styles.submitBtn, !canNext && styles.submitBtnDisabled)}
        onClick={() => {
          if (!canNext) {
            Taro.showToast({ title: sending ? '验证码发送中' : '请输入 11 位手机号', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
            return;
          }
          setSending(true);
          sendLoginSms(cleanPhone)
            .then((result) => {
              const url = redirect
                ? `/pages/auth/code/index?phone=${encodeURIComponent(cleanPhone)}&redirect=${encodeURIComponent(redirect)}${
                    result.devCode ? `&devCode=${encodeURIComponent(result.devCode)}` : ''
                  }`
                : `/pages/auth/code/index?phone=${encodeURIComponent(cleanPhone)}${
                    result.devCode ? `&devCode=${encodeURIComponent(result.devCode)}` : ''
                  }`;
              Taro.navigateTo({ url }).catch((err) => console.error('[Nav] code error', err));
            })
            .catch((err) => {
              console.error('[Auth] send sms error', err);
              Taro.showToast({ title: '验证码发送失败', icon: 'none' }).catch((toastErr) =>
                console.error('[Toast] showToast error', toastErr)
              );
            })
            .finally(() => setSending(false));
        }}
      >
        <Text className={classnames(styles.submitText, !canNext && styles.submitTextDisabled)}>
          {sending ? '发送中...' : '获取验证码'}
        </Text>
      </View>

      <Text className={styles.footer}>琥珀映画 · 专业摄影服务平台 · 技术支持</Text>
    </View>
  );
};

export default LoginPhonePage;
