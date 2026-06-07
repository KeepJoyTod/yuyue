import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Image, ScrollView } from '@tarojs/components';
import Taro from '@tarojs/taro';
import dayjs from 'dayjs';
import classnames from 'classnames';
import styles from './index.module.scss';
import { fetchServiceDetail } from '@/api/services';
import { fetchStoreSchedules } from '@/api/stores';
import Tag from '@/components/Tag';
import { formatDuration, formatPrice } from '@/utils/format';
import { useAuthStore } from '@/store/useAuthStore';
import { useBookingStore } from '@/store/useBookingStore';
import type { ScheduleDay, ScheduleSlot, ServiceItem } from '@/types/domain';

const ServiceDetailPage: React.FC = () => {
  const { serviceId } = Taro.getCurrentInstance().router?.params ?? {};
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const selectedStore = useBookingStore((s) => s.selectedStore);

  const [service, setService] = useState<ServiceItem>();
  const [schedules, setSchedules] = useState<ScheduleDay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeDate, setActiveDate] = useState<string>('');
  const [activeTime, setActiveTime] = useState<string>('');
  const [activeScheduleId, setActiveScheduleId] = useState<string>('');

  useEffect(() => {
    if (!serviceId) {
      setLoading(false);
      setError('服务不存在或已下架');
      return;
    }

    let alive = true;
    const startDate = dayjs().add(1, 'day').format('YYYY-MM-DD');

    setLoading(true);
    setError('');
    Promise.all([
      fetchServiceDetail(serviceId),
      fetchStoreSchedules({ storeId: selectedStore.id, serviceId, startDate, days: 7 })
    ])
      .then(([nextService, nextSchedules]) => {
        if (!alive) return;
        setService(nextService);
        setSchedules(nextSchedules);
        setActiveDate(nextSchedules.find((day) => day.slots.some((slot) => slot.available))?.date ?? nextSchedules[0]?.date ?? '');
        setActiveTime('');
        setActiveScheduleId('');
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[ServiceDetail] load detail error', err);
        setError('服务或档期加载失败，请稍后重试');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [selectedStore.id, serviceId]);

  const dateOptions = useMemo(() => schedules.map((day) => day.date), [schedules]);
  const timeSlots = useMemo<ScheduleSlot[]>(
    () => schedules.find((day) => day.date === activeDate)?.slots ?? [],
    [activeDate, schedules]
  );

  const canGoNext = !!service && !!activeDate && !!activeTime && !!activeScheduleId;

  if (loading || error || !service) {
    return (
      <View className={styles.container}>
        <Text className={styles.notFound}>{loading ? '服务加载中...' : error || '服务不存在或已下架'}</Text>
      </View>
    );
  }

  const selectedText = activeDate && activeTime ? `${activeDate} ${activeTime}` : '请选择日期与时间';

  return (
    <View className={styles.container}>
      <View className={styles.card}>
        <Image
          className={styles.cover}
          src={service.coverUrl}
          mode='aspectFill'
          onError={(err) => console.error('[Image] ServiceDetail cover error', { id: service.id, err })}
        />
        <View className={styles.main}>
          <View className={styles.titleRow}>
            <Text className={styles.title}>{service.name}</Text>
            <Text className={styles.price}>{formatPrice(service.price)}</Text>
          </View>
          <Text className={styles.meta}>{formatDuration(service.durationMin)} · 到店/到家以实际说明为准</Text>
          <Text className={styles.desc}>{service.desc}</Text>
          <View className={styles.tags}>
            {service.tags.map((t) => (
              <Tag key={t} text={t} tone='primary' />
            ))}
          </View>
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>选择日期</Text>
        <Text className={styles.sectionDesc}>优先选择你最方便的时间</Text>
        <ScrollView className={styles.dateScroll} scrollX>
          <View className={styles.dateRow}>
            {dateOptions.map((value) => {
              const isActive = value === activeDate;
              return (
                <View
                  key={value}
                  className={classnames(styles.dateItem, isActive && styles.dateItemActive)}
                  onClick={() => {
                    setActiveDate(value);
                    setActiveTime('');
                    setActiveScheduleId('');
                  }}
                >
                  <Text className={classnames(styles.dateText, isActive && styles.dateTextActive)}>
                    {dayjs(value).format('MM/DD')}
                  </Text>
                  <Text className={styles.dateSub}>{dayjs(value).format('ddd')}</Text>
                </View>
              );
            })}
          </View>
        </ScrollView>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>选择时间</Text>
        <Text className={styles.sectionDesc}>共 {timeSlots.filter((slot) => slot.available).length} 个可选时段</Text>
        <View className={styles.timeGrid}>
          {timeSlots.map((slot) => {
            const isActive = slot.scheduleId === activeScheduleId;
            return (
              <View
                key={slot.scheduleId}
                className={classnames(styles.timeItem, isActive && styles.timeItemActive, !slot.available && styles.timeItemDisabled)}
                onClick={() => {
                  if (!slot.available) return;
                  setActiveTime(slot.time);
                  setActiveScheduleId(slot.scheduleId);
                }}
              >
                <Text className={classnames(styles.timeText, isActive && styles.timeTextActive, !slot.available && styles.timeTextDisabled)}>
                  {slot.time}
                </Text>
              </View>
            );
          })}
          {timeSlots.length === 0 && <Text className={styles.notFound}>当前日期暂无可选档期</Text>}
        </View>
      </View>

      <View className={styles.bottomSpacer} />

      <View className={styles.fixedBar}>
        <View className={styles.fixedInner}>
          <Text className={styles.fixedText}>{selectedText}</Text>
          <View
            className={classnames(styles.fixedBtn, !canGoNext && styles.fixedBtnDisabled)}
            onClick={() => {
              if (!canGoNext) {
                Taro.showToast({ title: '请先选择时间', icon: 'none' }).catch((err) =>
                  console.error('[Toast] showToast error', err)
                );
                return;
              }
              const url = `/pages/booking/confirm/index?serviceId=${service.id}&date=${activeDate}&time=${activeTime}&scheduleId=${activeScheduleId}`;
              if (!isLoggedIn) {
                Taro.navigateTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent(url)}` }).catch((err) =>
                  console.error('[Nav] navigateTo login error', err)
                );
                return;
              }
              Taro.navigateTo({ url }).catch((err) => console.error('[Nav] navigateTo bookingConfirm error', err));
            }}
          >
            <Text className={classnames(styles.fixedBtnText, !canGoNext && styles.fixedBtnTextDisabled)}>
              去确认
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
};

export default ServiceDetailPage;
