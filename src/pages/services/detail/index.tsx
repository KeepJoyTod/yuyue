import React, { useMemo, useState } from 'react';
import { View, Text, Image, ScrollView } from '@tarojs/components';
import Taro from '@tarojs/taro';
import dayjs from 'dayjs';
import classnames from 'classnames';
import styles from './index.module.scss';
import { serviceList } from '@/data/services';
import Tag from '@/components/Tag';
import { formatDuration, formatPrice } from '@/utils/format';
import { useAuthStore } from '@/store/useAuthStore';

const timeSlots = ['10:00', '10:30', '11:00', '14:00', '14:30', '15:00', '19:00', '19:30'];

const ServiceDetailPage: React.FC = () => {
  const { serviceId } = Taro.getCurrentInstance().router?.params ?? {};
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);

  const service = useMemo(() => serviceList.find((s) => s.id === serviceId), [serviceId]);
  const dateOptions = useMemo(() => {
    const base = dayjs();
    return Array.from({ length: 7 }).map((_, idx) => base.add(idx + 1, 'day'));
  }, []);

  const [activeDate, setActiveDate] = useState<string>(() => dateOptions[0]?.format('YYYY-MM-DD') ?? '');
  const [activeTime, setActiveTime] = useState<string>('');

  const canGoNext = !!service && !!activeDate && !!activeTime;

  if (!service) {
    return (
      <View className={styles.container}>
        <Text className={styles.notFound}>服务不存在或已下架</Text>
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
            {dateOptions.map((d) => {
              const value = d.format('YYYY-MM-DD');
              const isActive = value === activeDate;
              return (
                <View
                  key={value}
                  className={classnames(styles.dateItem, isActive && styles.dateItemActive)}
                  onClick={() => setActiveDate(value)}
                >
                  <Text className={classnames(styles.dateText, isActive && styles.dateTextActive)}>
                    {d.format('MM/DD')}
                  </Text>
                  <Text className={styles.dateSub}>{d.format('ddd')}</Text>
                </View>
              );
            })}
          </View>
        </ScrollView>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>选择时间</Text>
        <Text className={styles.sectionDesc}>共 {timeSlots.length} 个可选时段</Text>
        <View className={styles.timeGrid}>
          {timeSlots.map((t) => {
            const isActive = t === activeTime;
            return (
              <View
                key={t}
                className={classnames(styles.timeItem, isActive && styles.timeItemActive)}
                onClick={() => setActiveTime(t)}
              >
                <Text className={classnames(styles.timeText, isActive && styles.timeTextActive)}>{t}</Text>
              </View>
            );
          })}
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
              const url = `/pages/booking/confirm/index?serviceId=${service.id}&date=${activeDate}&time=${activeTime}`;
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
