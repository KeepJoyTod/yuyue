import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Input, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { fetchServiceCategories, fetchServices } from '@/api/services';
import { fetchStores } from '@/api/stores';
import { useBookingStore } from '@/store/useBookingStore';
import type { ServiceCategory, ServiceCategoryId, ServiceItem, StoreItem } from '@/types/domain';

const BookingPage: React.FC = () => {
  const selectedStore = useBookingStore((s) => s.selectedStore);
  const setSelectedStore = useBookingStore((s) => s.setSelectedStore);

  const [mode, setMode] = useState<'packages' | 'stores'>('packages');
  const [keyword, setKeyword] = useState('');
  const [storeKeyword, setStoreKeyword] = useState('');
  const [storeTag, setStoreTag] = useState<'all' | '婚纱' | '写真' | '儿童'>('all');
  const [preferNearby, setPreferNearby] = useState(false);
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [stores, setStores] = useState<StoreItem[]>([]);
  const [loadingServices, setLoadingServices] = useState(true);
  const [loadingStores, setLoadingStores] = useState(true);
  const [serviceError, setServiceError] = useState('');
  const [storeError, setStoreError] = useState('');

  useEffect(() => {
    let alive = true;

    setLoadingServices(true);
    setServiceError('');
    Promise.all([fetchServiceCategories(), fetchServices()])
      .then(([nextCategories, nextServices]) => {
        if (!alive) return;
        setCategories(nextCategories);
        setServices(nextServices);
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[Services] load services error', err);
        setServiceError('套系加载失败，请稍后重试');
      })
      .finally(() => {
        if (alive) setLoadingServices(false);
      });

    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;

    setLoadingStores(true);
    setStoreError('');
    fetchStores()
      .then((nextStores) => {
        if (!alive) return;
        setStores(nextStores);
        if (!selectedStore?.id && nextStores[0]) {
          setSelectedStore({ id: nextStores[0].id, name: nextStores[0].name, address: nextStores[0].address });
        }
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[Services] load stores error', err);
        setStoreError('门店加载失败，请稍后重试');
      })
      .finally(() => {
        if (alive) setLoadingStores(false);
      });

    return () => {
      alive = false;
    };
  }, [selectedStore?.id, setSelectedStore]);

  const categoryNameById = useMemo(() => {
    const map = new Map(categories.map((c) => [c.id, c.name]));
    return (id: ServiceCategoryId) => map.get(id) ?? '';
  }, [categories]);

  const filtered = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    return services.filter((p) => {
      if (!kw) return true;
      return `${p.categoryName ?? categoryNameById(p.categoryId)} ${p.name}`.toLowerCase().includes(kw);
    });
  }, [categoryNameById, keyword, services]);

  const filteredStores = useMemo(() => {
    const kw = storeKeyword.trim().toLowerCase();
    const result = stores.filter((s) => {
      if (storeTag !== 'all' && !s.tags.includes(storeTag)) return false;
      if (!kw) return true;
      return `${s.name} ${s.address}`.toLowerCase().includes(kw);
    });
    if (!preferNearby) return result;
    return [...result].sort((a, b) => a.distanceKm - b.distanceKm);
  }, [preferNearby, storeKeyword, storeTag, stores]);

  if (mode === 'stores') {
    return (
      <View className={styles.container}>
        <View className={styles.storeHeader}>
          <View
            className={styles.backCircle}
            onClick={() => {
              setMode('packages');
            }}
          >
            <Text className={styles.backCircleIcon}>‹</Text>
          </View>
          <View className={styles.storeHeaderTitleWrap}>
            <Text className={styles.storeHeaderTitle}>选择门店</Text>
          </View>
          <View
            className={styles.nearbyBtn}
            onClick={() => {
              setPreferNearby(true);
              Taro.showToast({ title: '已按距离排序', icon: 'none' }).catch((err) =>
                console.error('[Toast] showToast error', err)
              );
            }}
          >
            <Text className={styles.nearbyIcon}>⌖</Text>
            <Text className={styles.nearbyText}>附近</Text>
          </View>
        </View>

        <View className={styles.storeSearchBar}>
          <Text className={styles.storeSearchIcon}>🔍</Text>
          <Input
            className={styles.storeSearchInput}
            placeholder='搜索门店名称或地区'
            placeholderClass={styles.searchPlaceholder}
            value={storeKeyword}
            onInput={(e) => setStoreKeyword(e.detail.value)}
            confirmType='search'
          />
        </View>

        <View className={styles.storeTags}>
          {(['all', '婚纱', '写真', '儿童'] as const).map((t) => {
            const active = t === storeTag;
            const text = t === 'all' ? '全部' : t;
            return (
              <View
                key={t}
                className={`${styles.storeTagChip} ${active ? styles.storeTagChipActive : ''}`}
                onClick={() => setStoreTag(t)}
              >
                <Text className={`${styles.storeTagChipText} ${active ? styles.storeTagChipTextActive : ''}`}>
                  {text}
                </Text>
              </View>
            );
          })}
        </View>

        <View className={styles.storeList}>
          {loadingStores && <Text className={styles.empty}>门店加载中...</Text>}
          {!loadingStores && storeError && <Text className={styles.empty}>{storeError}</Text>}
          {!loadingStores && !storeError && filteredStores.map((s) => (
            <View
              key={s.id}
              className={styles.storeItem}
              onClick={() => {
                setSelectedStore({ id: s.id, name: s.name, address: s.address });
                setMode('packages');
              }}
            >
              <View className={styles.storeCoverWrap}>
                <Image className={styles.storeCover} src={s.coverUrl} mode='aspectFill' />
                {s.hasSlotToday && (
                  <View className={styles.slotBadge}>
                    <Text className={styles.slotBadgeText}>今日有档期</Text>
                  </View>
                )}
              </View>

              <View className={styles.storeMeta}>
                <View className={styles.storeMetaTop}>
                  <Text className={styles.storeName}>{s.name}</Text>
                  <Text className={styles.storeDistance}>{s.distanceKm.toFixed(1)}km</Text>
                </View>
                <View className={styles.storeAddrRow}>
                  <Text className={styles.storeAddrIcon}>📍</Text>
                  <Text className={styles.storeAddrText}>{s.address}</Text>
                </View>
                <View className={styles.storeInfoRow}>
                  <Text className={styles.storeStar}>★</Text>
                  <Text className={styles.storeRating}>{s.rating.toFixed(1)}</Text>
                  <Text className={styles.storeReviews}>({s.reviews})</Text>
                  <Text className={styles.storeDot}>·</Text>
                  <Text className={styles.storeHoursIcon}>🕒</Text>
                  <Text className={styles.storeHours}>{s.hours}</Text>
                </View>
                <View className={styles.storePills}>
                  {s.tags.map((t) => (
                    <View key={t} className={styles.storePill}>
                      <Text className={styles.storePillText}>{t}</Text>
                    </View>
                  ))}
                </View>
              </View>
            </View>
          ))}

          {!loadingStores && !storeError && filteredStores.length === 0 && <Text className={styles.empty}>没有匹配的门店</Text>}
        </View>
      </View>
    );
  }

  return (
    <View className={styles.container}>
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

      <View
        className={styles.storeCard}
        onClick={() => {
          setMode('stores');
        }}
      >
        <View className={styles.storeIcon}>
          <Text className={styles.storeIconText}>📍</Text>
        </View>
        <View className={styles.storeInfo}>
          <Text className={styles.storeTitle}>{selectedStore.name}</Text>
          <Text className={styles.storeSub}>{selectedStore.address}</Text>
        </View>
        <Text className={styles.storeArrow}>›</Text>
      </View>

      <View className={styles.sectionHeader}>
        <Text className={styles.sectionTitle}>热门套系</Text>
        <View
          className={styles.sectionMore}
          onClick={() => {
            setKeyword('');
            Taro.showToast({ title: '已显示全部套系', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <Text className={styles.sectionMoreText}>全部</Text>
          <Text className={styles.sectionMoreArrow}>›</Text>
        </View>
      </View>

      <View className={styles.list}>
        {loadingServices && <Text className={styles.empty}>套系加载中...</Text>}
        {!loadingServices && serviceError && <Text className={styles.empty}>{serviceError}</Text>}
        {!loadingServices && !serviceError && filtered.map((p) => (
          <View key={p.id} className={styles.packageCard}>
            <Image
              className={styles.packageImg}
              src={p.coverUrl}
              mode='aspectFill'
              onError={(err) => console.error('[Image] package error', { id: p.id, err })}
            />
            <View className={styles.packageMain}>
              <View className={styles.packageTag}>
                <Text className={styles.packageTagText}>{p.categoryName ?? categoryNameById(p.categoryId)}</Text>
              </View>
              <Text className={styles.packageName}>{p.name}</Text>
              <View className={styles.packageMeta}>
                <Text className={styles.packageStar}>★</Text>
                <Text className={styles.packageRating}>{(p.rating ?? 4.8).toFixed(1)}</Text>
              </View>
              <Text className={styles.packagePrice}>¥{p.price}</Text>
            </View>
            <View
              className={styles.bookBtn}
              onClick={() => {
                Taro.navigateTo({ url: `/pages/services/detail/index?serviceId=${p.id}` }).catch((err) =>
                  console.error('[Nav] navigateTo detail error', err)
                );
              }}
            >
              <Text className={styles.bookBtnText}>立即预约</Text>
            </View>
          </View>
        ))}
        {!loadingServices && !serviceError && filtered.length === 0 && <Text className={styles.empty}>没有匹配的套系</Text>}
      </View>
    </View>
  );
};

export default BookingPage;
