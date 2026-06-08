import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Input, Swiper, SwiperItem, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { fetchServiceCategories, fetchServices } from '@/api/services';
import type { ServiceCategory, ServiceCategoryId, ServiceItem } from '@/types/domain';

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

const categoryIcons: Partial<Record<ServiceCategoryId, string>> = {
  wedding: '💍',
  portrait: '👗',
  kids: '🧸',
  business: '🧑‍💼',
  family: '👨‍👩‍👧‍👦'
};

const HomePage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;

    setLoading(true);
    setError('');
    Promise.all([fetchServiceCategories(), fetchServices()])
      .then(([nextCategories, nextServices]) => {
        if (!alive) return;
        setCategories(nextCategories);
        setServices(nextServices);
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[Home] load catalog error', err);
        setError('首页数据加载失败，请稍后重试');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, []);

  const banners = useMemo<HomeBanner[]>(() => {
    return services.slice(0, 3).map((service, index) => ({
      id: service.id,
      coverUrl: service.coverUrl,
      badge: index === 0 ? '热门套系' : service.categoryName ?? '推荐套系',
      title: service.name,
      subTitle: `${service.desc} · ¥${service.price}`
    }));
  }, [services]);

  const entryList = useMemo<HomeEntry[]>(() => {
    return categories.slice(0, 8).map((category) => ({
      id: category.id,
      icon: categoryIcons[category.id] ?? '✨',
      text: category.name
    }));
  }, [categories]);

  const activityList = useMemo<HomeEntry[]>(
    () => [
      { id: 'a_001', icon: '📷', text: `在售套系\n${services.length} 个` },
      { id: 'a_002', icon: '🏷️', text: `服务分类\n${categories.length} 类` },
      { id: 'a_003', icon: '📅', text: '可约档期\n实时查询' },
      { id: 'a_004', icon: '📍', text: '到店门店\n在线选择' }
    ],
    [categories.length, services.length]
  );

  const works = useMemo<HomeWork[]>(() => {
    return services.slice(0, 6).map((service) => ({
      id: service.id,
      coverUrl: service.coverUrl,
      tag: service.categoryName ?? service.name,
      likeCount: Math.round((service.rating ?? 4.8) * 100)
    }));
  }, [services]);

  const catalogSummary = useMemo(() => {
    return `当前在售 ${services.length} 个套系 · 覆盖 ${categories.length} 类服务`;
  }, [categories.length, services.length]);

  const goServices = () => {
    Taro.switchTab({ url: '/pages/services/index' }).catch((err) => console.error('[Nav] switchTab services error', err));
  };

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
          onConfirm={goServices}
        />
      </View>

      <View className={styles.bannerWrap}>
        {loading && <Text className={styles.empty}>首页数据加载中...</Text>}
        {!loading && error && <Text className={styles.empty}>{error}</Text>}
        {!loading && !error && banners.length > 0 && (
          <Swiper className={styles.bannerSwiper} circular autoplay>
            {banners.map((b) => (
              <SwiperItem key={b.id}>
                <View
                  className={styles.bannerItem}
                  onClick={() => {
                    Taro.navigateTo({ url: `/pages/services/detail/index?serviceId=${b.id}` }).catch((err) =>
                      console.error('[Nav] navigateTo detail error', err)
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
        )}
      </View>

      <View className={styles.entryGrid}>
        {entryList.map((it) => (
          <View
            key={it.id}
            className={styles.entryItem}
            onClick={goServices}
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
            goServices();
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
            onClick={goServices}
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
            <Text className={styles.giftTitle}>预约服务</Text>
            <Text className={styles.giftDesc}>{catalogSummary}</Text>
          </View>
        </View>
        <View
          className={styles.giftBtn}
          onClick={() => {
            goServices();
          }}
        >
          <Text className={styles.giftBtnText}>去预约</Text>
        </View>
      </View>

      <View className={styles.sectionHeader}>
        <Text className={styles.sectionTitle}>样片作品</Text>
        <View
          className={styles.sectionMore}
          onClick={() => {
            goServices();
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
              Taro.navigateTo({ url: `/pages/services/detail/index?serviceId=${w.id}` }).catch((err) =>
                console.error('[Nav] navigateTo detail error', err)
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
