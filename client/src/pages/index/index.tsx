import React, { useMemo, useState } from 'react';
import { View, Text, Input, Swiper, SwiperItem, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';

interface HomeBanner {
  id: string;
  coverUrl: string;
  badge: string;
  title: string;
  subTitle: string;
}

interface HomeEntry {
  id: string;
  icon: string;
  text: string;
}

interface HomeWork {
  id: string;
  coverUrl: string;
  tag: string;
  likeCount: number;
}

const HomePage: React.FC = () => {
  const [keyword, setKeyword] = useState('');

  const banners = useMemo<HomeBanner[]>(
    () => [
      {
        id: 'bn_001',
        coverUrl: 'https://picsum.photos/id/338/750/400',
        badge: '限时特惠',
        title: '新人专属礼包',
        subTitle: '首次预约立减 200 元'
      },
      {
        id: 'bn_002',
        coverUrl: 'https://picsum.photos/id/177/750/400',
        badge: '热门套系',
        title: '梦境 · 白纱系列',
        subTitle: '档期充足，支持分期'
      }
    ],
    []
  );

  const entryList = useMemo<HomeEntry[]>(
    () => [
      { id: 'e_001', icon: '💍', text: '婚纱摄影' },
      { id: 'e_002', icon: '👗', text: '写真套系' },
      { id: 'e_003', icon: '🧸', text: '儿童摄影' },
      { id: 'e_004', icon: '🎓', text: '毕业照' },
      { id: 'e_005', icon: '🧑‍💼', text: '商务形象' },
      { id: 'e_006', icon: '👨‍👩‍👧‍👦', text: '全家福' },
      { id: 'e_007', icon: '🌸', text: '孕期纪念' },
      { id: 'e_008', icon: '✨', text: '更多' }
    ],
    []
  );

  const activityList = useMemo<HomeEntry[]>(
    () => [
      { id: 'a_001', icon: '🎫', text: '优惠券\n3张可用' },
      { id: 'a_002', icon: '⚡', text: '限时秒杀\n10:00开始' },
      { id: 'a_003', icon: '👥', text: '拼团优惠\n2人成团' },
      { id: 'a_004', icon: '🪓', text: '预约活动\n邀好友砍价' }
    ],
    []
  );

  const works = useMemo<HomeWork[]>(
    () => [
      { id: 'w_001', coverUrl: 'https://picsum.photos/id/1027/750/500', tag: '写真', likeCount: 2341 },
      { id: 'w_002', coverUrl: 'https://picsum.photos/id/91/750/500', tag: '婚纱', likeCount: 1876 },
      { id: 'w_003', coverUrl: 'https://picsum.photos/id/64/750/500', tag: '写真', likeCount: 1543 },
      { id: 'w_004', coverUrl: 'https://picsum.photos/id/338/750/500', tag: '艺术', likeCount: 2108 },
      { id: 'w_005', coverUrl: 'https://picsum.photos/id/177/750/500', tag: '商务', likeCount: 987 },
      { id: 'w_006', coverUrl: 'https://picsum.photos/id/1025/750/500', tag: '亲子', likeCount: 3012 }
    ],
    []
  );

  return (
    <View className={styles.container}>
      <View className={styles.topBar}>
        <View className={styles.brand}>
          <View className={styles.brandIcon}>
            <Text className={styles.brandIconText}>📷</Text>
          </View>
          <Text className={styles.brandText}>琥珀映画</Text>
        </View>
        <View className={styles.topRight}>
          <View className={styles.location}>
            <Text className={styles.locationIcon}>📍</Text>
            <Text className={styles.locationText}>上海</Text>
          </View>
          <View
            className={styles.noticeBtn}
            onClick={() => {
              Taro.showToast({ title: '通知（待接入）', icon: 'none' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <Text className={styles.noticeIcon}>🔔</Text>
          </View>
        </View>
      </View>

      <View className={styles.searchBar}>
        <Text className={styles.searchIcon}>🔍</Text>
        <Input
          className={styles.searchInput}
          placeholder='搜索套系、门店'
          placeholderClass={styles.searchPlaceholder}
          value={keyword}
          onInput={(e) => setKeyword(e.detail.value)}
          confirmType='search'
        />
      </View>

      <View className={styles.bannerWrap}>
        <Swiper className={styles.bannerSwiper} circular autoplay>
          {banners.map((b) => (
            <SwiperItem key={b.id}>
              <View
                className={styles.bannerItem}
                onClick={() => {
                  Taro.showToast({ title: b.title, icon: 'none' }).catch((err) =>
                    console.error('[Toast] showToast error', err)
                  );
                }}
              >
                <Image
                  className={styles.bannerImg}
                  src={b.coverUrl}
                  mode='aspectFill'
                  onError={(err) => console.error('[Image] banner error', { id: b.id, err })}
                />
                <View className={styles.bannerMask} />
                <View className={styles.bannerContent}>
                  <View className={styles.bannerBadge}>
                    <Text className={styles.bannerBadgeText}>{b.badge}</Text>
                  </View>
                  <Text className={styles.bannerTitle}>{b.title}</Text>
                  <Text className={styles.bannerSub}>{b.subTitle}</Text>
                </View>
              </View>
            </SwiperItem>
          ))}
        </Swiper>
      </View>

      <View className={styles.entryGrid}>
        {entryList.map((it) => (
          <View
            key={it.id}
            className={styles.entryItem}
            onClick={() => {
              Taro.switchTab({ url: '/pages/services/index' }).catch((err) =>
                console.error('[Nav] switchTab booking error', err)
              );
            }}
          >
            <View className={styles.entryIcon}>
              <Text className={styles.entryIconText}>{it.icon}</Text>
            </View>
            <Text className={styles.entryText}>{it.text}</Text>
          </View>
        ))}
      </View>

      <View className={styles.sectionHeader}>
        <Text className={styles.sectionTitle}>专属活动</Text>
        <View
          className={styles.sectionMore}
          onClick={() => {
            Taro.showToast({ title: '查看全部（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <Text className={styles.sectionMoreText}>查看全部</Text>
          <Text className={styles.sectionMoreArrow}>›</Text>
        </View>
      </View>

      <View className={styles.activityGrid}>
        {activityList.map((it) => (
          <View
            key={it.id}
            className={styles.activityItem}
            onClick={() => {
              Taro.showToast({ title: '活动（待接入）', icon: 'none' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <View className={styles.activityIcon}>
              <Text className={styles.activityIconText}>{it.icon}</Text>
            </View>
            <Text className={styles.activityText}>{it.text}</Text>
          </View>
        ))}
      </View>

      <View className={styles.giftCard}>
        <View className={styles.giftLeft}>
          <View className={styles.giftIcon}>
            <Text className={styles.giftIconText}>🎁</Text>
          </View>
          <View className={styles.giftInfo}>
            <Text className={styles.giftTitle}>新人礼包</Text>
            <Text className={styles.giftDesc}>注册即得 3 张优惠券 + 200 积分</Text>
          </View>
        </View>
        <View
          className={styles.giftBtn}
          onClick={() => {
            Taro.showToast({ title: '领取成功（演示）', icon: 'success' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <Text className={styles.giftBtnText}>立即领取</Text>
        </View>
      </View>

      <View className={styles.sectionHeader}>
        <Text className={styles.sectionTitle}>样片作品</Text>
        <View
          className={styles.sectionMore}
          onClick={() => {
            Taro.showToast({ title: '更多作品（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <Text className={styles.sectionMoreText}>更多作品</Text>
          <Text className={styles.sectionMoreArrow}>›</Text>
        </View>
      </View>

      <View className={styles.workGrid}>
        {works.map((w) => (
          <View
            key={w.id}
            className={styles.workItem}
            onClick={() => {
              Taro.showToast({ title: '作品详情（待接入）', icon: 'none' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <Image
              className={styles.workImg}
              src={w.coverUrl}
              mode='aspectFill'
              onError={(err) => console.error('[Image] work error', { id: w.id, err })}
            />
            <View className={styles.workMask} />
            <View className={styles.workBottom}>
              <Text className={styles.workTag}>{w.tag}</Text>
              <Text className={styles.workLike}>♡ {w.likeCount}</Text>
            </View>
          </View>
        ))}
      </View>
    </View>
  );
};

export default HomePage;
