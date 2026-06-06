import React, { useMemo, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';

const LoginPhonePage: React.FC = () => {
  const { redirect: redirectParam } = Taro.getCurrentInstance().router?.params ?? {};
  const [phone, setPhone] = useState('');

  const cleanPhone = useMemo(() => phone.replace(/\s/g, ''), [phone]);
  const canNext = cleanPhone.length >= 8;
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
        提示：<Text className={styles.hintStrong}>测试账号 138 0013 8000</Text>（验证码任意6位）
      </Text>

      <View
        className={classnames(styles.submitBtn, !canNext && styles.submitBtnDisabled)}
        onClick={() => {
          if (!canNext) {
            Taro.showToast({ title: '请输入手机号', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
            return;
          }
          const url = redirect
            ? `/pages/auth/code/index?phone=${encodeURIComponent(cleanPhone)}&redirect=${encodeURIComponent(redirect)}`
            : `/pages/auth/code/index?phone=${encodeURIComponent(cleanPhone)}`;
          Taro.navigateTo({ url }).catch((err) => console.error('[Nav] code error', err));
        }}
      >
        <Text className={classnames(styles.submitText, !canNext && styles.submitTextDisabled)}>获取验证码</Text>
      </View>

      <Text className={styles.footer}>琥珀映画 · 专业摄影服务平台 · 技术支持</Text>
    </View>
  );
};

export default LoginPhonePage;
