import React from 'react';
import { View, Text, Image } from '@tarojs/components';
import styles from './index.module.scss';
import type { ServiceItem } from '@/types/domain';
import Tag from '@/components/Tag';
import { formatDuration, formatPrice } from '@/utils/format';

export interface ServiceCardProps {
  service: ServiceItem;
  onClick?: () => void;
  onBookClick?: () => void;
}

const ServiceCard: React.FC<ServiceCardProps> = ({ service, onClick, onBookClick }) => {
  return (
    <View className={styles.card} onClick={onClick}>
      <Image
        className={styles.cover}
        src={service.coverUrl}
        mode='aspectFill'
        onError={(err) => console.error('[Image] ServiceCard cover error', { id: service.id, err })}
      />
      <View className={styles.main}>
        <View className={styles.topRow}>
          <Text className={styles.name}>{service.name}</Text>
          <Text className={styles.meta}>
            {formatDuration(service.durationMin)} · {formatPrice(service.price)}
          </Text>
        </View>
        <Text className={styles.desc}>{service.desc}</Text>
        <View className={styles.bottomRow}>
          <View className={styles.tags}>
            {service.tags.slice(0, 3).map((t) => (
              <Tag key={t} text={t} />
            ))}
          </View>
          <View
            className={styles.bookBtn}
            onClick={(e) => {
              e.stopPropagation();
              onBookClick?.();
            }}
          >
            <Text className={styles.bookBtnText}>预约</Text>
          </View>
        </View>
      </View>
    </View>
  );
};

export default ServiceCard;

