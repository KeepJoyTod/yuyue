import React, { useEffect, useMemo } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import type { BookingStatus } from '@/types/domain';
import Tag from '@/components/Tag';
import { useBookingStore } from '@/store/useBookingStore';
import { formatDuration, formatPrice } from '@/utils/format';

const statusText: Record<BookingStatus, string> = {
  pending: '待支付',
  confirmed: '已预约',
  completed: '已完成',
  cancelled: '已取消'
};

const statusTone: Record<BookingStatus, 'default' | 'primary' | 'success' | 'warning' | 'danger'> = {
  pending: 'warning',
  confirmed: 'primary',
  completed: 'success',
  cancelled: 'default'
};

const OrderDetailPage: React.FC = () => {
  const { id } = Taro.getCurrentInstance().router?.params ?? {};

  const hydrateDefaults = useBookingStore((s) => s.hydrateDefaults);
  const bookings = useBookingStore((s) => s.bookings);
  const updateStatus = useBookingStore((s) => s.updateStatus);

  useEffect(() => {
    hydrateDefaults();
  }, [hydrateDefaults]);

  const booking = useMemo(() => bookings.find((b) => b.id === id), [bookings, id]);

  if (!booking) {
    return (
      <View className={styles.container}>
        <Text className={styles.notFound}>订单不存在或已被清空</Text>
      </View>
    );
  }

  return (
    <View className={styles.container}>
      <View className={styles.card}>
        <View className={styles.titleRow}>
          <Text className={styles.title}>{booking.serviceName}</Text>
          <View className={styles.status}>
            <Tag text={statusText[booking.status]} tone={statusTone[booking.status]} />
          </View>
        </View>

        <View className={styles.divider} />

        <View className={styles.infoRow}>
          <Text className={styles.label}>预约时间</Text>
          <Text className={styles.value}>
            {booking.date} {booking.time}
          </Text>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.label}>门店</Text>
          <Text className={styles.value}>{booking.storeName || '未选择门店'}</Text>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.label}>时长</Text>
          <Text className={styles.value}>{formatDuration(booking.durationMin)}</Text>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.label}>联系人</Text>
          <Text className={styles.value}>{booking.userName}</Text>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.label}>手机号</Text>
          <Text className={styles.value}>{booking.userPhone}</Text>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.label}>金额</Text>
          <Text className={`${styles.value} ${styles.price}`}>{formatPrice(booking.price)}</Text>
        </View>
      </View>

      <View className={styles.actions}>
        {booking.status === 'confirmed' && (
          <View
            className={styles.primaryBtn}
            onClick={() => {
              updateStatus(booking.id, 'completed');
              Taro.showToast({ title: '已标记完成', icon: 'success' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <Text className={styles.primaryText}>完成服务</Text>
          </View>
        )}

        <View
          className={styles.ghostBtn}
          onClick={() => {
            Taro.switchTab({ url: '/pages/orders/index' }).catch((err) =>
              console.error('[Nav] switchTab orders error', err)
            );
          }}
        >
          <Text className={styles.ghostText}>返回订单</Text>
        </View>

        {booking.status === 'pending' && (
          <View
            className={styles.dangerBtn}
            onClick={() => {
              Taro.showModal({
                title: '确认取消？',
                content: '取消后可重新选择时间再预约。',
                confirmText: '取消订单',
                cancelText: '再想想'
              })
                .then((res) => {
                  if (!res.confirm) return;
                  updateStatus(booking.id, 'cancelled');
                  Taro.showToast({ title: '已取消', icon: 'success' }).catch((err) =>
                    console.error('[Toast] showToast error', err)
                  );
                })
                .catch((err) => console.error('[Modal] showModal error', err));
            }}
          >
            <Text className={styles.dangerText}>取消订单</Text>
          </View>
        )}
      </View>
    </View>
  );
};

export default OrderDetailPage;
