import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import type { BookingItem, BookingStatus } from '@/types/domain';
import { useBookingStore } from '@/store/useBookingStore';
import { useAuthStore } from '@/store/useAuthStore';

type FilterKey = 'all' | BookingStatus;

const filterList: { key: FilterKey; text: string }[] = [
  { key: 'all', text: '全部' },
  { key: 'pending', text: '待支付' },
  { key: 'confirmed', text: '已预约' },
  { key: 'completed', text: '已完成' },
  { key: 'cancelled', text: '已取消' }
];

const statusText: Record<BookingStatus, string> = {
  pending: '待支付',
  confirmed: '已预约',
  completed: '已完成',
  cancelled: '已取消'
};

const buildOrderNo = (createdAt: string, fallbackId: string) => {
  const ts = Date.parse(createdAt);
  if (!Number.isFinite(ts)) return `ORD${fallbackId.replace(/[^0-9]/g, '').slice(-10) || '0000000000'}`;
  const d = new Date(ts);
  const yyyy = d.getFullYear().toString();
  const mm = (d.getMonth() + 1).toString().padStart(2, '0');
  const dd = d.getDate().toString().padStart(2, '0');
  const tail = (ts % 100000).toString().padStart(5, '0');
  return `ORD${yyyy}${mm}${dd}${tail}`;
};

const OrdersPage: React.FC = () => {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const authUser = useAuthStore((s) => s.user);
  const loginMethod = useAuthStore((s) => s.loginMethod);
  const hydrateDefaults = useBookingStore((s) => s.hydrateDefaults);
  const bookings = useBookingStore((s) => s.bookings);
  const updateStatus = useBookingStore((s) => s.updateStatus);
  const removeBooking = useBookingStore((s) => s.removeBooking);

  const [filter, setFilter] = useState<FilterKey>('all');

  useEffect(() => {
    hydrateDefaults();
  }, [hydrateDefaults]);

  const list = useMemo<BookingItem[]>(() => {
    if (filter === 'all') return bookings;
    return bookings.filter((o) => o.status === filter);
  }, [bookings, filter]);

  const phoneText = useMemo(() => {
    if (loginMethod !== 'phone') return '';
    const phone = authUser?.phone;
    if (!phone) return '';
    return phone.replace(/^(\d{3})\d+(\d{4})$/, '$1****$2');
  }, [authUser?.phone, loginMethod]);

  const goLogin = () => {
    Taro.navigateTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent('/pages/orders/index')}` }).catch((err) =>
      console.error('[Nav] login error', err)
    );
  };

  if (!isLoggedIn) {
    return (
      <View className={styles.container}>
        <View className={styles.titleRow}>
          <Text className={styles.title}>我的订单</Text>
        </View>
        <View className={styles.list}>
          <Text className={styles.empty}>登录后查看订单与预约状态</Text>
          <View className={styles.actions}>
            <View className={styles.primaryBtn} onClick={goLogin}>
              <Text className={styles.primaryBtnText}>去登录</Text>
            </View>
          </View>
        </View>
      </View>
    );
  }

  return (
    <View className={styles.container}>
      <View className={styles.titleRow}>
        <Text className={styles.title}>我的订单</Text>
        {!!phoneText && <Text className={styles.phone}>{phoneText}</Text>}
      </View>

      <View className={styles.filterBar}>
        {filterList.map((it) => {
          const isActive = it.key === filter;
          return (
            <View
              key={it.key}
              className={classnames(styles.filterItem, isActive && styles.filterItemActive)}
              onClick={() => setFilter(it.key)}
            >
              <Text className={classnames(styles.filterText, isActive && styles.filterTextActive)}>{it.text}</Text>
            </View>
          );
        })}
      </View>

      <View className={styles.list}>
        {list.map((o) => {
          const orderNo = buildOrderNo(o.createdAt, o.id);
          const dateTime = `${o.date} ${o.time}`;
          const storeName = o.storeName || '未选择门店';
          const isPending = o.status === 'pending';
          const isCancelled = o.status === 'cancelled';
          const isCompleted = o.status === 'completed';

          const primaryText = isPending ? '去支付' : '⌁ 核销码';
          const ghostText = isPending ? '取消' : isCompleted ? '评价' : '查看';

          return (
          <View key={o.id} className={styles.card} onClick={() => {
            Taro.navigateTo({ url: `/pages/orders/detail/index?id=${o.id}` }).catch((err) =>
              console.error('[Nav] navigateTo orderDetail error', err)
            );
          }}>
            <View className={styles.cardTop}>
              <Text className={styles.orderNo}>{orderNo}</Text>
              <View
                className={classnames(
                  styles.statusBadge,
                  o.status === 'completed' && styles.statusBadgeDone,
                  o.status === 'cancelled' && styles.statusBadgeCancelled
                )}
              >
                <Text
                  className={classnames(
                    styles.statusText,
                    o.status === 'completed' && styles.statusTextDone,
                    o.status === 'cancelled' && styles.statusTextCancelled
                  )}
                >
                  {statusText[o.status]}
                </Text>
              </View>
            </View>

            <View className={styles.cardMain}>
              <Image
                className={styles.cover}
                src={o.serviceCoverUrl}
                mode='aspectFill'
                onError={(err) => console.error('[Image] order cover error', { id: o.id, err })}
              />
              <View className={styles.info}>
                <Text className={styles.name}>{o.serviceName}</Text>
                <Text className={styles.sub}>{storeName}</Text>
                <Text className={styles.sub}>📅 {dateTime}</Text>
                <Text className={styles.price}>¥{o.price.toLocaleString()}</Text>
              </View>
            </View>

            <View className={styles.actions}>
              <View
                className={styles.primaryBtn}
                onClick={() => {
                  if (isCancelled) {
                    Taro.switchTab({ url: '/pages/services/index' }).catch((err) =>
                      console.error('[Nav] switchTab booking error', err)
                    );
                    return;
                  }
                  if (isPending) {
                    updateStatus(o.id, 'confirmed');
                    Taro.showToast({ title: '支付成功（演示）', icon: 'success' }).catch((err) =>
                      console.error('[Toast] showToast error', err)
                    );
                    return;
                  }
                  Taro.showToast({ title: '核销码（待接入）', icon: 'none' }).catch((err) =>
                    console.error('[Toast] showToast error', err)
                  );
                }}
              >
                <Text className={styles.primaryBtnText}>{isCancelled ? '再次预约' : primaryText}</Text>
              </View>
              <View
                className={styles.ghostBtn}
                onClick={() => {
                  if (isPending) {
                    Taro.showModal({
                      title: '确认取消？',
                      content: '取消后可重新选择时间再预约。',
                      confirmText: '取消订单',
                      cancelText: '再想想'
                    })
                      .then((res) => {
                        if (!res.confirm) return;
                        updateStatus(o.id, 'cancelled');
                        Taro.showToast({ title: '已取消', icon: 'success' }).catch((err) =>
                          console.error('[Toast] showToast error', err)
                        );
                      })
                      .catch((err) => console.error('[Modal] showModal error', err));
                    return;
                  }

                  if (isCancelled) {
                    removeBooking(o.id);
                    Taro.showToast({ title: '已删除', icon: 'success' }).catch((err) =>
                      console.error('[Toast] showToast error', err)
                    );
                    return;
                  }

                  Taro.showToast({ title: `${ghostText}（待接入）`, icon: 'none' }).catch((err) =>
                    console.error('[Toast] showToast error', err)
                  );
                }}
              >
                <Text className={styles.ghostBtnText}>{ghostText}</Text>
              </View>
            </View>
          </View>
        );})}
        {list.length === 0 && <Text className={styles.empty}>这里还没有内容</Text>}
      </View>
    </View>
  );
};

export default OrdersPage;
