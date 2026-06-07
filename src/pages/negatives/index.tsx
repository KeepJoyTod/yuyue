import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';
import { fetchNegativeSessions, type NegativeSession } from '@/api/negatives';
import { useAuthStore } from '@/store/useAuthStore';

type NegativeStatus = 'pendingSelect' | 'pendingSubmit' | 'completed';
type TabKey = 'all' | NegativeStatus;

const tabList: { key: TabKey; text: string }[] = [
  { key: 'all', text: '全部' },
  { key: 'pendingSelect', text: '待选片' },
  { key: 'pendingSubmit', text: '待提交' },
  { key: 'completed', text: '已完成' }
];

const statusText: Record<NegativeStatus, string> = {
  pendingSelect: '待选片',
  pendingSubmit: '待提交',
  completed: '已完成'
};

const NegativesPage: React.FC = () => {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const [mode, setMode] = useState<'list' | 'select'>('list');
  const [tab, setTab] = useState<TabKey>('all');
  const [sessions, setSessions] = useState<NegativeSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<string[]>([]);

  useEffect(() => {
    if (!isLoggedIn) {
      setSessions([]);
      return;
    }

    let alive = true;
    setLoading(true);
    setError('');
    fetchNegativeSessions()
      .then((nextSessions) => {
        if (alive) setSessions(nextSessions);
      })
      .catch((err) => {
        if (!alive) return;
        console.error('[Negatives] load sessions error', err);
        setError('底片加载失败，请稍后重试');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [isLoggedIn]);

  const list = useMemo(() => {
    if (tab === 'all') return sessions;
    return sessions.filter((s) => s.status === tab);
  }, [sessions, tab]);

  const activeSession = useMemo(() => {
    if (!activeSessionId) return null;
    return sessions.find((s) => s.id === activeSessionId) ?? null;
  }, [activeSessionId, sessions]);

  const goLogin = () => {
    Taro.navigateTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent('/pages/negatives/index')}` }).catch((err) =>
      console.error('[Nav] login error', err)
    );
  };

  const goSelect = (session: NegativeSession) => {
    if (session.status === 'completed') {
      Taro.showToast({ title: '精修已完成，可下载', icon: 'none' }).catch((err) =>
        console.error('[Toast] showToast error', err)
      );
      return;
    }

    const limit = session.refinedCount ?? 0;
    const initialCount = Math.min(limit, session.selectedCount ?? 0);
    const initialSelected = session.photos.slice(0, initialCount).map((photo) => photo.id);
    setSelectedPhotoIds(initialSelected);
    setActiveSessionId(session.id);
    setMode('select');
  };

  const leaveSelect = () => {
    if (!activeSessionId) {
      setMode('list');
      return;
    }
    const nextSelectedCount = selectedPhotoIds.length;
    setSessions((prev) =>
      prev.map((s) => {
        if (s.id !== activeSessionId) return s;
        const nextStatus: NegativeStatus =
          nextSelectedCount > 0 ? (s.status === 'completed' ? 'completed' : 'pendingSubmit') : 'pendingSelect';
        return { ...s, selectedCount: nextSelectedCount, status: nextStatus };
      })
    );
    if (tab !== 'all') {
      if (tab === 'pendingSelect' && nextSelectedCount > 0) setTab('pendingSubmit');
      else if (tab === 'pendingSubmit' && nextSelectedCount === 0) setTab('pendingSelect');
    }
    setMode('list');
  };

  const togglePhoto = (photoId: string) => {
    const limit = activeSession?.refinedCount ?? 0;
    setSelectedPhotoIds((prev) => {
      if (prev.includes(photoId)) return prev.filter((id) => id !== photoId);
      if (limit > 0 && prev.length >= limit) {
        Taro.showToast({ title: `最多可选 ${limit} 张`, icon: 'none' }).catch((err) =>
          console.error('[Toast] showToast error', err)
        );
        return prev;
      }
      return [...prev, photoId];
    });
  };

  const photos = useMemo(() => {
    if (!activeSession) return [];
    return activeSession.photos.slice(0, 60);
  }, [activeSession]);

  if (mode === 'select' && isLoggedIn && activeSession) {
    const limit = activeSession.refinedCount ?? 0;
    const progress = limit > 0 ? Math.min(1, Math.max(0, selectedPhotoIds.length / limit)) : 0;

    return (
      <View className={styles.selectPage}>
        <View className={styles.selectPanel}>
          <View className={styles.selectHeader}>
            <View className={styles.backCircle} onClick={leaveSelect}>
              <Text className={styles.backCircleIcon}>‹</Text>
            </View>

            <View className={styles.selectHeaderText}>
              <Text className={styles.selectTitle}>{activeSession.name}·选片</Text>
              <Text className={styles.selectSub}>
                已选 {selectedPhotoIds.length} / {limit} 张
              </Text>
            </View>
          </View>

          <View className={styles.selectProgressBar}>
            <View className={styles.selectProgressFill} style={{ width: `${Math.round(progress * 100)}%` }} />
          </View>

          <View className={styles.photoGrid}>
            {photos.map((p) => {
              const order = selectedPhotoIds.indexOf(p.id) + 1;
              const isSelected = order > 0;
              return (
                <View key={p.id} className={styles.photoCell} onClick={() => togglePhoto(p.id)}>
                  <Image
                    className={styles.photoImg}
                    src={p.src}
                    mode='aspectFill'
                    onError={(err) => console.error('[Image] photo error', { id: p.id, err })}
                  />

                  <View className={classnames(styles.photoDot, isSelected && styles.photoDotSelected)}>
                    {isSelected && <Text className={styles.photoDotText}>{order}</Text>}
                  </View>
                </View>
              );
            })}
          </View>
        </View>
      </View>
    );
  }

  return (
    <View className={styles.container}>
      <View className={styles.header}>
        <Text className={styles.title}>我的底片</Text>
      </View>

      {!isLoggedIn && (
        <View className={styles.list}>
          <View className={styles.card}>
            <Text className={styles.empty}>登录后查看底片、选片与下载进度</Text>
            <View className={styles.actions}>
              <View className={styles.primaryBtn} onClick={goLogin}>
                <Text className={styles.primaryBtnText}>去登录</Text>
              </View>
            </View>
          </View>
        </View>
      )}

      {isLoggedIn && (
        <>
          <View className={styles.tabs}>
            {tabList.map((t) => {
              const isActive = t.key === tab;
              return (
                <View
                  key={t.key}
                  className={classnames(styles.tabItem, isActive && styles.tabItemActive)}
                  onClick={() => setTab(t.key)}
                >
                  <Text className={classnames(styles.tabText, isActive && styles.tabTextActive)}>{t.text}</Text>
                </View>
              );
            })}
          </View>

          <View className={styles.list}>
            {loading && <Text className={styles.empty}>底片加载中...</Text>}
            {!loading && error && <Text className={styles.empty}>{error}</Text>}
            {!loading && !error && list.map((s) => {
              const done = s.status === 'completed';
              const isPendingSelect = s.status === 'pendingSelect';
              const isPendingSubmit = s.status === 'pendingSubmit';
              const selectedCount = s.selectedCount ?? 0;
              const refinedCount = s.refinedCount ?? 0;
              const progress = refinedCount > 0 ? Math.min(1, Math.max(0, selectedCount / refinedCount)) : 0;
              const progressText = done ? '精修完成，可下载' : `已选 ${selectedCount} / ${refinedCount} 张`;
              const primaryText = done ? '下载精修片' : isPendingSelect ? '开始选片' : '继续选片';

              return (
                <View key={s.id} className={styles.card}>
                  <View className={styles.topRow}>
                    <View className={styles.thumbWrap}>
                      <Image
                        className={styles.thumb}
                        src={s.coverUrl}
                        mode='aspectFill'
                        onError={(err) => console.error('[Image] Negative cover error', { id: s.id, err })}
                      />
                      <View className={styles.countBadge}>
                        <Text className={styles.countBadgeText}>{s.totalCount}张</Text>
                      </View>
                    </View>

                    <View className={styles.info}>
                      <View className={styles.nameRow}>
                        <Text className={styles.name}>{s.name}</Text>
                        <View className={classnames(styles.statusBadge, done && styles.statusBadgeDone)}>
                          <Text className={classnames(styles.statusBadgeText, done && styles.statusBadgeTextDone)}>
                            {statusText[s.status]}
                          </Text>
                        </View>
                      </View>

                      <Text className={styles.meta}>拍摄日期：{s.shootDate}</Text>

                      <View className={styles.progressRow}>
                        <Text className={styles.meta}>{progressText}</Text>
                        <Text className={styles.meta}>共{s.totalCount}张原片</Text>
                      </View>

                      {!done && (
                        <View className={styles.progressBar}>
                          <View className={styles.progressBarFill} style={{ width: `${Math.round(progress * 100)}%` }} />
                        </View>
                      )}

                      <View className={styles.downloadRow}>
                        <View className={styles.downloadIcon} />
                        <Text className={styles.meta}>在线下载</Text>
                      </View>
                    </View>
                  </View>

                  <View className={styles.actions}>
                    <View
                      className={styles.primaryBtn}
                      onClick={() => {
                        if (!done) {
                          goSelect(s);
                          return;
                        }
                        Taro.showToast({ title: '开始下载（待接入）', icon: 'none' }).catch((err) =>
                          console.error('[Toast] showToast error', err)
                        );
                      }}
                    >
                      <View className={styles.primaryBtnIcon} />
                      <Text className={styles.primaryBtnText}>{primaryText}</Text>
                    </View>

                    <View
                      className={styles.ghostBtn}
                      onClick={() => {
                        if (isPendingSubmit) {
                          Taro.showToast({ title: '待提交（待接入）', icon: 'none' }).catch((err) =>
                            console.error('[Toast] showToast error', err)
                          );
                          return;
                        }
                        Taro.showToast({ title: '查看详情（待接入）', icon: 'none' }).catch((err) =>
                          console.error('[Toast] showToast error', err)
                        );
                      }}
                    >
                      <Text className={styles.ghostBtnText}>查看详情</Text>
                      <View className={styles.chevronRight} />
                    </View>
                  </View>
                </View>
              );
            })}
            {!loading && !error && list.length === 0 && <Text className={styles.empty}>这里还没有底片</Text>}
          </View>
        </>
      )}
    </View>
  );
};

export default NegativesPage;
