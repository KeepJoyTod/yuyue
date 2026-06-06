import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { serviceList } from '@/data/services';
import { useBookingStore } from '@/store/useBookingStore';
import { useAuthStore } from '@/store/useAuthStore';
import { formatDuration, formatPrice } from '@/utils/format';

const BookingConfirmPage: React.FC = () => {
  const { serviceId, date, time } = Taro.getCurrentInstance().router?.params ?? {};
  const service = useMemo(() => serviceList.find((s) => s.id === serviceId), [serviceId]);

  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const authUser = useAuthStore((s) => s.user);
  const createBooking = useBookingStore((s) => s.createBooking);
  const selectedStore = useBookingStore((s) => s.selectedStore);

  const [userName, setUserName] = useState('');
  const [userPhone, setUserPhone] = useState('');

  const canSubmit = !!service && !!date && !!time && userName.trim().length > 0 && userPhone.trim().length >= 8;

  useEffect(() => {
    if (isLoggedIn) return;
    if (!serviceId || !date || !time) return;
    const redirect = `/pages/booking/confirm/index?serviceId=${encodeURIComponent(serviceId)}&date=${encodeURIComponent(
      date
    )}&time=${encodeURIComponent(time)}`;
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

  if (!service || !date || !time) {
    return (
      <View className={styles.container}>
        <Text className={styles.notFound}>参数缺失，请返回重新选择</Text>
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

        <Text className={styles.tips}>温馨提示：此项目为演示版，数据仅保存在本地。</Text>
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

              try {
                const next = createBooking({
                  serviceId: service.id,
                  serviceName: service.name,
                  serviceCoverUrl: service.coverUrl,
                  storeId: selectedStore.id,
                  storeName: selectedStore.name,
                  storeAddress: selectedStore.address,
                  price: service.price,
                  durationMin: service.durationMin,
                  userName: userName.trim(),
                  userPhone: userPhone.trim(),
                  date,
                  time
                });

                Taro.showToast({ title: '预约已提交', icon: 'success' }).catch((err) =>
                  console.error('[Toast] showToast error', err)
                );
                Taro.switchTab({ url: '/pages/orders/index' }).catch((err) =>
                  console.error('[Nav] switchTab orders error', err)
                );
                console.info('[Booking] created and redirect', { id: next.id });
              } catch (err) {
                console.error('[Booking] createBooking error', err);
                Taro.showToast({ title: '提交失败，请重试', icon: 'none' }).catch((toastErr) =>
                  console.error('[Toast] showToast error', toastErr)
                );
              }
            }}
          >
            <Text className={classnames(styles.submitText, !canSubmit && styles.submitTextDisabled)}>提交预约</Text>
          </View>
        </View>
      </View>
    </View>
  );
};

export default BookingConfirmPage;
