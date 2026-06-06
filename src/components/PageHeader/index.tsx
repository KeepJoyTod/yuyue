import React from 'react';
import { View, Text } from '@tarojs/components';
import styles from './index.module.scss';

export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  rightText?: string;
  onRightClick?: () => void;
}

const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, rightText, onRightClick }) => {
  return (
    <View className={styles.container}>
      <View className={styles.left}>
        <Text className={styles.title}>{title}</Text>
        {!!subtitle && <Text className={styles.subtitle}>{subtitle}</Text>}
      </View>
      {!!rightText && (
        <View className={styles.right} onClick={onRightClick}>
          <Text className={styles.rightText}>{rightText}</Text>
        </View>
      )}
    </View>
  );
};

export default PageHeader;

