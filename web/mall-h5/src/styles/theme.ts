import type { ConfigProviderThemeVars } from 'vant'

/**
 * H5 设计规范唯一来源：颜色、圆角、阴影。
 * SCSS 侧通过 variables.scss 中的 CSS 变量（--mall-*）引用同一套值，页面不得写死颜色。
 */
export const theme = {
  primary: '#ff4142',
  primaryEnd: '#ff6a3d',
  primaryLight: '#fff1ef',
  orange: '#ff9900',
  orangeStart: '#ffc53d',
  gold: '#f7c56b',
  success: '#07c160',
  warning: '#ff976a',
  blue: '#1989fa',
  text: '#1f1f1f',
  textSecondary: '#666666',
  textTertiary: '#999999',
  border: '#eeeeee',
  borderStrong: '#dcdee0',
  disabled: '#c8c9cc',
  bg: '#f5f6f8',
  card: '#ffffff',
  outer: '#e8e9ec',
  radiusSm: '6px',
  radius: '10px',
  radiusLg: '14px',
} as const

export const gradient = `linear-gradient(90deg, ${theme.primary}, ${theme.primaryEnd})`
export const orangeGradient = `linear-gradient(90deg, ${theme.orangeStart}, ${theme.orange})`

/** Vant ConfigProvider 统一主题 */
export const themeVars: ConfigProviderThemeVars = {
  primaryColor: theme.primary,
  dangerColor: theme.primary,
  buttonPrimaryBackground: gradient,
  buttonPrimaryBorderColor: 'transparent',
  buttonDangerBackground: gradient,
  buttonDangerBorderColor: 'transparent',
  buttonRoundRadius: '999px',
  tabbarItemActiveColor: theme.primary,
  tabsBottomBarColor: theme.primary,
  checkboxCheckedIconColor: theme.primary,
  radioCheckedIconColor: theme.primary,
  switchOnBackground: theme.primary,
  sliderActiveBackground: theme.primary,
  rateIconFullColor: theme.orange,
  stepperButtonRoundThemeColor: theme.primary,
  sidebarSelectedBorderColor: theme.primary,
  tagPrimaryColor: theme.primary,
  tagDangerColor: theme.primary,
  badgeBackground: theme.primary,
  countDownTextColor: theme.primary,
  navBarIconColor: theme.text,
  navBarTextColor: theme.text,
  searchContentBackground: theme.bg,
  pullRefreshHeadTextColor: theme.textTertiary,
  stepActiveColor: theme.primary,
  stepFinishLineColor: theme.primary,
  submitBarPriceColor: theme.primary,
  actionBarButtonWarningColor: orangeGradient,
  actionBarButtonDangerColor: gradient,
}

/** 写入 CSS 变量，供 SCSS / 行内样式统一引用 */
export function applyCssVars() {
  const root = document.documentElement.style
  const map: Record<string, string> = {
    '--mall-primary': theme.primary,
    '--mall-primary-end': theme.primaryEnd,
    '--mall-primary-light': theme.primaryLight,
    '--mall-gradient': gradient,
    '--mall-orange': theme.orange,
    '--mall-orange-gradient': orangeGradient,
    '--mall-gold': theme.gold,
    '--mall-success': theme.success,
    '--mall-warning': theme.warning,
    '--mall-blue': theme.blue,
    '--mall-text': theme.text,
    '--mall-text-2': theme.textSecondary,
    '--mall-text-3': theme.textTertiary,
    '--mall-border': theme.border,
    '--mall-border-strong': theme.borderStrong,
    '--mall-disabled': theme.disabled,
    '--mall-bg': theme.bg,
    '--mall-card': theme.card,
    '--mall-outer': theme.outer,
    '--mall-radius-sm': theme.radiusSm,
    '--mall-radius': theme.radius,
    '--mall-radius-lg': theme.radiusLg,
  }
  Object.entries(map).forEach(([k, v]) => root.setProperty(k, v))
}
