import React from 'react';
import { View, Text, Image } from '@tarojs/components';
import type { BookingItem, BookingStatus } from '@/types/domain';
import styles from './index.module.scss';
import Tag from '@/components/Tag';
import { formatDuration, formatPrice } from '@/utils/format';

export interface OrderCardProps {
  booking: BookingItem;
  onClick?: () => void;
  onCancel?: () => void;
}

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

const OrderCard: React.FC<OrderCardProps> = ({ booking, onClick, onCancel }) => {
  return (
    <View className={styles.card} onClick={onClick}>
      <Image
        className={styles.cover}
        src={booking.serviceCoverUrl}
        mode='aspectFill'
        onError={(err) => console.error('[Image] OrderCard cover error', { id: booking.id, err })}
      />
      <View className={styles.main}>
        <View className={styles.header}>
          <Text className={styles.name}>{booking.serviceName}</Text>
          <Tag text={statusText[booking.status]} tone={statusTone[booking.status]} />
        </View>
        <Text className={styles.sub}>
          {booking.date} · {booking.time} · {formatDuration(booking.durationMin)}
        </Text>
        <View className={styles.footer}>
          <Text className={styles.price}>{formatPrice(booking.price)}</Text>
          {booking.status === 'pending' && (
            <View
              className={styles.cancelBtn}
              onClick={(e) => {
                e.stopPropagation();
                onCancel?.();
              }}
            >
              <Text className={styles.cancelBtnText}>取消</Text>
            </View>
          )}
        </View>
      </View>
    </View>
  );
};

export default OrderCard;

