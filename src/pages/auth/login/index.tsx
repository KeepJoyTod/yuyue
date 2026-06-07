import React, { useMemo, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';

const LoginPage: React.FC = () => {
  const { redirect: redirectParam } = Taro.getCurrentInstance().router?.params ?? {};

  const [agreed, setAgreed] = useState(false);
  const [showConsentBar, setShowConsentBar] = useState(false);

  const redirect = useMemo(() => {
    if (!redirectParam) return '';
    try {
      return decodeURIComponent(redirectParam);
    } catch {
      return String(redirectParam);
    }
  }, [redirectParam]);

  const handleWechat = () => {
    if (!agreed) {
      setShowConsentBar(true);
      return;
    }
    Taro.showToast({ title: '微信登录待接入，请使用手机号登录', icon: 'none' }).catch((err) =>
      console.error('[Toast] showToast error', err)
    );
  };

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

      <Text className={styles.title}>登录 / 注册</Text>
      <Text className={styles.subtitle}>登录后可查看预约订单、底片及会员权益</Text>

      <View className={styles.wechatBtn} onClick={handleWechat}>
        <Text className={styles.wechatBtnText}>💚 微信一键登录</Text>
      </View>

      <View className={styles.dividerRow}>
        <View className={styles.dividerLine} />
        <Text className={styles.dividerText}>或</Text>
        <View className={styles.dividerLine} />
      </View>

      <View
        className={styles.phoneEntry}
        onClick={() => {
          const url = redirect
            ? `/pages/auth/phone/index?redirect=${encodeURIComponent(redirect)}`
            : '/pages/auth/phone/index';
          Taro.navigateTo({ url }).catch((err) => console.error('[Nav] phone error', err));
        }}
      >
        <View className={styles.phoneEntryLeft}>
          <Text className={styles.phoneIcon}>📞</Text>
          <Text className={styles.phoneEntryText}>手机号登录 / 注册</Text>
        </View>
        <Text className={styles.arrow}>›</Text>
      </View>

      <View className={styles.agreeRow}>
        <View
          className={classnames(styles.checkbox, agreed && styles.checkboxChecked)}
          onClick={() => {
            setAgreed((v) => !v);
            setShowConsentBar(false);
          }}
        >
          {agreed && <Text className={styles.checkMark}>✓</Text>}
        </View>
        <Text className={styles.agreeText}>
          我已阅读并同意 <Text className={styles.agreeHighlight}>《用户服务协议》</Text> 和{' '}
          <Text className={styles.agreeHighlight}>《隐私政策》</Text>
        </Text>
      </View>

      {showConsentBar && (
        <View className={styles.consentBar}>
          <Text className={styles.consentHint}>请先勾选同意用户协议以后再登录</Text>
          <View
            className={styles.consentBtn}
            onClick={() => {
              setAgreed(true);
              setShowConsentBar(false);
              Taro.showToast({ title: '微信登录待接入，请使用手机号登录', icon: 'none' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <Text className={styles.consentBtnText}>同意并继续</Text>
          </View>
        </View>
      )}

      <Text className={styles.footer}>琥珀映画 · 专业摄影服务平台 · 技术支持</Text>
    </View>
  );
};

export default LoginPage;
