import React, { useEffect, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { fetchServiceDetail } from '@/api/services';
import { createRemoteBooking } from '@/api/orders';
import { useBookingStore } from '@/store/useBookingStore';
import { useAuthStore } from '@/store/useAuthStore';
import { formatDuration, formatPrice } from '@/utils/format';
import type { ServiceItem } from '@/types/domain';

const BookingConfirmPage: React.FC = () => {
  const { serviceId, date, time, scheduleId } = Taro.getCurrentInstance().router?.params ?? {};

  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const authUser = useAuthStore((s) => s.user);
  const selectedStore = useBookingStore((s) => s.selectedStore);

  const [userName, setUserName] = useState('');
  const [userPhone, setUserPhone] = useState('');
  const [service, setService] = useState<ServiceItem>();
  const [loadingService, setLoadingService] = useState(true);
  const [serviceError, setServiceError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const canSubmit =
    !!service && !!date && !!time && !!scheduleId && userName.trim().length > 0 && userPhone.trim().length >= 8 && !submitting;

  useEffect(() => {
    if (!serviceId) {
      setLoadingService(false);
      setServiceError('参数缺失，请返回重新选择');
      return;
    }

    let alive = true;
    setLoadingService(true);
    setServiceError('');
    fetchServiceDetail(serviceId)
      .then((nextService) => {
        if (alive) setService(nextService);
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[BookingConfirm] load service error', err);
        setServiceError('服务加载失败，请返回重新选择');
      })
      .finally(() => {
        if (alive) setLoadingService(false);
      });

    return () => {
      alive = false;
    };
  }, [serviceId]);

  useEffect(() => {
    if (isLoggedIn) return;
    if (!serviceId || !date || !time) return;
    const redirect = `/pages/booking/confirm/index?serviceId=${encodeURIComponent(serviceId)}&date=${encodeURIComponent(
      date
    )}&time=${encodeURIComponent(time)}${scheduleId ? `&scheduleId=${encodeURIComponent(scheduleId)}` : ''}`;
    Taro.redirectTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent(redirect)}` }).catch((err) =>
      console.error('[Nav] redirectTo login error', err)
    );
    Taro.showToast({ title: '请先登录再预约', icon: 'none' }).catch((err) => console.error('[Toast] error', err));
  }, [date, isLoggedIn, serviceId, time]);

  useEffect(() => {
    if (!authUser) return;
    if (!userName) {
      const nextName = authUser.realName || authUser.nickName;
      if (nextName) setUserName(nextName);
    }
    if (!userPhone && authUser.phone) setUserPhone(authUser.phone);
  }, [authUser, userName, userPhone]);

  if (loadingService || serviceError || !service || !date || !time) {
    return (
      <View className={styles.container}>
        <Text className={styles.notFound}>{loadingService ? '服务加载中...' : serviceError || '参数缺失，请返回重新选择'}</Text>
      </View>
    );
  }

  return (
    <View className={styles.container}>
      <View className={styles.card}>
        <Text className={styles.title}>确认信息</Text>
        <Text className={styles.sub}>提交后会生成订单，可在「订单」中查看状态</Text>

        <View className={styles.summaryRow}>
          <View className={styles.summaryLeft}>
            <Text className={styles.summaryName}>{service.name}</Text>
            <Text className={styles.summaryMeta}>
              {date} {time} · {formatDuration(service.durationMin)}
            </Text>
            <Text className={styles.summaryMeta}>门店：{selectedStore.name}</Text>
          </View>
          <Text className={styles.summaryPrice}>{formatPrice(service.price)}</Text>
        </View>

        <View className={styles.form}>
          <View className={styles.field}>
            <Text className={styles.label}>联系人</Text>
            <Input
              className={styles.input}
              placeholder='请输入姓名'
              value={userName}
              onInput={(e) => setUserName(e.detail.value)}
            />
          </View>

          <View className={styles.field}>
            <Text className={styles.label}>手机号</Text>
            <Input
              className={styles.input}
              placeholder='用于联系与确认'
              type='number'
              value={userPhone}
              onInput={(e) => setUserPhone(e.detail.value)}
            />
          </View>
        </View>

        <Text className={styles.tips}>温馨提示：提交后将生成真实后端订单，可在「订单」中查看状态。</Text>
      </View>

      <View className={styles.bottomSpacer} />

      <View className={styles.submitBar}>
        <View className={styles.submitInner}>
          <View
            className={classnames(styles.submitBtn, !canSubmit && styles.submitBtnDisabled)}
            onClick={() => {
              if (!canSubmit) {
                Taro.showToast({ title: '请完善信息', icon: 'none' }).catch((err) =>
                  console.error('[Toast] showToast error', err)
                );
                return;
              }

              setSubmitting(true);
              createRemoteBooking({
                serviceId: service.id,
                storeId: selectedStore.id,
                scheduleId: String(scheduleId),
                contactName: userName.trim(),
                contactPhone: userPhone.trim()
              })
                .then((next) => {
                  Taro.showToast({ title: '预约已提交', icon: 'success' }).catch((err) =>
                    console.error('[Toast] showToast error', err)
                  );
                  Taro.switchTab({ url: '/pages/orders/index' }).catch((err) =>
                    console.error('[Nav] switchTab orders error', err)
                  );
                  console.info('[Booking] created and redirect', { id: next.id });
                })
                .catch((err) => {
                  console.error('[Booking] createBooking error', err);
                  Taro.showToast({ title: '提交失败，请重试', icon: 'none' }).catch((toastErr) =>
                    console.error('[Toast] showToast error', toastErr)
                  );
                })
                .finally(() => setSubmitting(false));
            }}
          >
            <Text className={classnames(styles.submitText, !canSubmit && styles.submitTextDisabled)}>
              {submitting ? '提交中...' : '提交预约'}
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
};

export default BookingConfirmPage;
