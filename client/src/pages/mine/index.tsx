import React, { useMemo } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import styles from './index.module.scss';
import { useAuthStore } from '@/store/useAuthStore';

const MinePage: React.FC = () => {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const authUser = useAuthStore((s) => s.user);
  const loginMethod = useAuthStore((s) => s.loginMethod);
  const logout = useAuthStore((s) => s.logout);

  const user = useMemo(() => {
    if (!authUser) {
      return {
        name: '未登录',
        phone: '',
        levelName: '普通会员',
        growth: 0,
        nextNeed: 380
      };
    }
    const showPhone = loginMethod === 'phone' && !!authUser.phone;
    return {
      name: authUser.realName || authUser.nickName || '微信用户',
      phone: showPhone ? authUser.phone!.replace(/^(\d{3})\d+(\d{4})$/, '$1****$2') : '',
      levelName: '普通会员',
      growth: authUser.realName ? 120 : 0,
      nextNeed: authUser.realName ? 380 : 500
    };
  }, [authUser, loginMethod]);

  const stats = useMemo(
    () => [
      { key: 'coupon', label: '优惠券', value: '3张' },
      { key: 'point', label: '积分', value: '120' },
      { key: 'balance', label: '余额', value: '¥0' },
      { key: 'card', label: '次卡', value: '3次' }
    ],
    []
  );

  const goLogin = () => {
    Taro.navigateTo({ url: `/pages/auth/login/index?redirect=${encodeURIComponent('/pages/mine/index')}` }).catch((err) =>
      console.error('[Nav] login error', err)
    );
  };

  return (
    <View className={styles.container}>
      {!isLoggedIn && (
        <>
          <View className={styles.unauthCard}>
            <View className={styles.unauthLeft}>
              <View className={styles.unauthAvatar}>
                <Text className={styles.unauthAvatarText}>👤</Text>
              </View>
              <View className={styles.unauthInfo}>
                <Text className={styles.unauthTitle}>未登录</Text>
                <Text className={styles.unauthSub}>登录后查看会员权益</Text>
              </View>
            </View>
            <View className={styles.loginBtn} onClick={goLogin}>
              <Text className={styles.loginBtnText}>➜ 登录</Text>
            </View>
          </View>

          <View className={styles.statsGridUnauth}>
            {stats.map((s) => (
              <View key={s.key} className={styles.statItemUnauth}>
                <Text className={styles.statLabelUnauth}>{s.label}</Text>
              </View>
            ))}
          </View>

          <View className={styles.listCard}>
            {[
              { id: 'coupon', icon: '🎫', title: '我的优惠券' },
              { id: 'card', icon: '💳', title: '会员卡/次卡' },
              { id: 'point', icon: '🪙', title: '积分明细' },
              { id: 'balance', icon: '💰', title: '余额与明细' }
            ].map((it) => (
              <View key={it.id} className={styles.row} onClick={goLogin}>
                <View className={styles.rowLeft}>
                  <Text className={styles.rowIcon}>{it.icon}</Text>
                  <Text className={styles.rowTitle}>{it.title}</Text>
                </View>
                <View className={styles.rowRight}>
                  <Text className={styles.rowRightText}>登录查看</Text>
                  <Text className={styles.rowArrow}>›</Text>
                </View>
              </View>
            ))}
          </View>

          <View className={styles.listCard}>
            {[
              { id: 'rate', icon: '💬', title: '服务评价' },
              { id: 'gift', icon: '🎁', title: '推荐有礼', right: '邀请好友得积分' },
              { id: 'help', icon: '❓', title: '帮助中心' },
              { id: 'setting', icon: '⚙️', title: '账号设置' }
            ].map((it) => (
              <View key={it.id} className={styles.row} onClick={goLogin}>
                <View className={styles.rowLeft}>
                  <Text className={styles.rowIcon}>{it.icon}</Text>
                  <Text className={styles.rowTitle}>{it.title}</Text>
                </View>
                <View className={styles.rowRight}>
                  {!!it.right && <Text className={styles.rowRightText}>{it.right}</Text>}
                  <Text className={styles.rowArrow}>›</Text>
                </View>
              </View>
            ))}
          </View>

          <View className={styles.primaryLoginBtn} onClick={goLogin}>
            <Text className={styles.primaryLoginText}>➜ 立即登录</Text>
          </View>
        </>
      )}

      {isLoggedIn && (
      <>
      <View className={styles.userCard}>
        <View className={styles.userTop}>
          <View className={styles.avatar}>
            <Text className={styles.avatarText}>🙂</Text>
          </View>
          <View className={styles.userInfo}>
            <Text className={styles.userName}>{user.name}</Text>
            {!!user.phone && <Text className={styles.userPhone}>手机：{user.phone}</Text>}
            <View className={styles.userBadges}>
              <View className={styles.badge}>
                <Text className={styles.badgeText}>👑 {user.levelName}</Text>
              </View>
              <Text className={styles.growthText}>成长值 {user.growth}</Text>
            </View>
          </View>
          <Text className={styles.userArrow}>›</Text>
        </View>
      </View>

      <View className={styles.levelCard}>
        <View className={styles.levelRow}>
          <Text className={styles.levelLeft}>{user.levelName}</Text>
          <Text className={styles.levelRight}>距银牌会员还差 {user.nextNeed} 成长值</Text>
        </View>
        <View className={styles.progressTrack}>
          <View className={styles.progressFill} style={{ width: `${Math.min(100, (user.growth / (user.growth + user.nextNeed)) * 100)}%` }} />
        </View>
      </View>

      <View className={styles.statsGrid}>
        {stats.map((s) => (
          <View key={s.key} className={styles.statItem}>
            <Text className={styles.statValue}>{s.value}</Text>
            <Text className={styles.statLabel}>{s.label}</Text>
          </View>
        ))}
      </View>

      <View className={styles.listCard}>
        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '我的优惠券（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>🎫</Text>
            <Text className={styles.rowTitle}>我的优惠券</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowRightText}>3张可用</Text>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '会员卡/次卡（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>💳</Text>
            <Text className={styles.rowTitle}>会员卡/次卡</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowRightText}>1张有效</Text>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '积分明细（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>🪙</Text>
            <Text className={styles.rowTitle}>积分明细</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowRightText}>120分</Text>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '余额与明细（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>💰</Text>
            <Text className={styles.rowTitle}>余额与明细</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowRightText}>¥0</Text>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>
      </View>

      <View className={styles.listCard}>
        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '服务评价（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>💬</Text>
            <Text className={styles.rowTitle}>服务评价</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '推荐有礼（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>🎁</Text>
            <Text className={styles.rowTitle}>推荐有礼</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowRightText}>邀请好友得积分</Text>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.showToast({ title: '帮助中心（待接入）', icon: 'none' }).catch((err) =>
              console.error('[Toast] showToast error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>❓</Text>
            <Text className={styles.rowTitle}>帮助中心</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>

        <View
          className={styles.row}
          onClick={() => {
            Taro.navigateTo({ url: '/pages/misc/settings/index' }).catch((err) =>
              console.error('[Nav] navigateTo settings error', err)
            );
          }}
        >
          <View className={styles.rowLeft}>
            <Text className={styles.rowIcon}>⚙️</Text>
            <Text className={styles.rowTitle}>账号设置</Text>
          </View>
          <View className={styles.rowRight}>
            <Text className={styles.rowArrow}>›</Text>
          </View>
        </View>
      </View>

      <View
        className={styles.logoutBtn}
        onClick={() => {
          logout();
          Taro.showToast({ title: '已退出', icon: 'none' }).catch((err) => console.error('[Toast] error', err));
        }}
      >
        <Text className={styles.logoutText}>↩ 退出登录</Text>
      </View>
      </>
      )}
    </View>
  );
};

export default MinePage;
