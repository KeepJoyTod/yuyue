export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/services/index',
    'pages/negatives/index',
    'pages/orders/index',
    'pages/mine/index',
    'pages/auth/login/index',
    'pages/auth/phone/index',
    'pages/auth/code/index',
    'pages/auth/realName/index',
    'pages/services/detail/index',
    'pages/booking/confirm/index',
    'pages/orders/detail/index',
    'pages/misc/settings/index',
    'pages/misc/about/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#0B0B10',
    navigationBarTitleText: '琥珀映画',
    navigationBarTextStyle: 'white'
  },
  tabBar: {
    color: '#86909C',
    selectedColor: '#D8AE5D',
    backgroundColor: '#1A1822',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页'
      },
      {
        pagePath: 'pages/services/index',
        text: '预约'
      },
      {
        pagePath: 'pages/negatives/index',
        text: '底片'
      },
      {
        pagePath: 'pages/orders/index',
        text: '订单'
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的'
      }
    ]
  }
})
