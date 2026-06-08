import React, { useMemo } from 'react';
import { View, Text } from '@tarojs/components';
import styles from './index.module.scss';

const AboutPage: React.FC = () => {
  const build = useMemo(() => new Date().toISOString().slice(0, 10), []);

  return (
    <View className={styles.container}>
      <View className={styles.card}>
        <Text className={styles.title}>关于</Text>
        <Text className={styles.desc}>这是一个基于 Taro + React 的摄影预约小程序项目。</Text>
        <Text className={styles.muted}>Build: {build}</Text>
      </View>
    </View>
  );
};

export default AboutPage;

