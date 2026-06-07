import React from 'react';
import { View, Text } from '@tarojs/components';
import styles from './index.module.scss';

export interface SectionTitleProps {
  title: string;
  desc?: string;
  actionText?: string;
  onActionClick?: () => void;
}

const SectionTitle: React.FC<SectionTitleProps> = ({ title, desc, actionText, onActionClick }) => {
  return (
    <View className={styles.container}>
      <View className={styles.left}>
        <Text className={styles.title}>{title}</Text>
        {!!desc && <Text className={styles.desc}>{desc}</Text>}
      </View>
      {!!actionText && (
        <View className={styles.action} onClick={onActionClick}>
          <Text className={styles.actionText}>{actionText}</Text>
        </View>
      )}
    </View>
  );
};

export default SectionTitle;

