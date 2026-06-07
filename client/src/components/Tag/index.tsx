import React from 'react';
import classnames from 'classnames';
import { Text } from '@tarojs/components';
import styles from './index.module.scss';

export interface TagProps {
  text: string;
  tone?: 'default' | 'primary' | 'success' | 'warning' | 'danger';
  className?: string;
}

const Tag: React.FC<TagProps> = ({ text, tone = 'default', className }) => {
  return <Text className={classnames(styles.tag, styles[tone], className)}>{text}</Text>;
};

export default Tag;

