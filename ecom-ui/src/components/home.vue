<template>
  <div class="home">
    <!-- 顶部横幅 -->
    <section class="hero">
      <div class="hero-grid"></div>
      <div class="hero-glow"></div>
      <div class="hero-main">
        <p class="hello">{{ helloText }}</p>
        <h1 class="hero-title">{{ heroTitle }}</h1>
        <p class="hero-desc">{{ heroDesc }}</p>
        <div class="pills">
          <span class="pill" v-for="role in roleLabels" :key="role">{{ role }}</span>
          <span class="pill ghost">权限 {{ permissions.length }} 项</span>
          <span class="pill ghost">{{ today }}</span>
        </div>
      </div>
      <div class="hero-side">
        <span class="live"><i></i>系统运行中</span>
        <span class="since">本次登录 {{ loginTime }}</span>
      </div>
    </section>

    <!-- 库存预警提示 -->
    <section class="alert" v-if="canSeeAlert && alertCount > 0">
      <span class="alert-ico">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M12 4.5 21 19H3z" stroke-linejoin="round" />
          <path d="M12 10v4M12 16.6v.2" stroke-linecap="round" />
        </svg>
      </span>
      <div class="alert-text">
        <b>库存预警待处理</b>
        <span>有 {{ alertCount }} 条预警未处理，库存低于阈值或长期积压，建议尽快补货或促销</span>
      </div>
      <el-button class="alert-btn" size="small" @click="$router.push('/stock/alert')">去处理</el-button>
    </section>

    <!-- 今日经营概览 -->
    <section class="card today" v-if="canSeeDashboard">
      <div class="card-head">
        <h3>今日经营</h3>
        <div class="head-right">
          <span class="live-dot"><i></i>实时</span>
          <el-link type="primary" :underline="false" @click="$router.push('/dashboard')">去数据看板</el-link>
        </div>
      </div>
      <div class="kpis" v-if="realtime">
        <div class="kpi" v-for="kpi in kpiList" :key="kpi.label">
          <span class="kpi-label">{{ kpi.label }}</span>
          <strong class="kpi-value">{{ kpi.value }}<em v-if="kpi.unit">{{ kpi.unit }}</em></strong>
          <span v-if="kpi.mom" class="mom" :class="kpi.trend">{{ kpi.mom }}</span>
          <span v-else class="mom hint">{{ kpi.hint }}</span>
        </div>
      </div>
      <el-skeleton v-else :rows="1" animated />
      <p class="kpi-foot">口径：支付成功订单（不含未支付与已退款）· 后端缓存 5 分钟，看板页可手动重算</p>
    </section>

    <div class="row2">
      <!-- 账号信息 -->
      <section class="card">
        <div class="card-head">
          <h3>账号信息</h3>
          <span class="tag-line">{{ userTypeText === '运营' ? '运营侧账号' : (userTypeText === '消费者' ? '消费者账号' : '') }}</span>
        </div>
        <dl class="info">
          <div>
            <dt>登录名</dt>
            <dd>{{ user.username || '-' }}</dd>
          </div>
          <div>
            <dt>昵称</dt>
            <dd>{{ user.nickName || '-' }}</dd>
          </div>
          <div>
            <dt>用户类型</dt>
            <dd>{{ userTypeText }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>
              <span class="role" v-for="role in roles" :key="role">{{ role }}</span>
              <span v-if="!roles.length" class="muted">-</span>
            </dd>
          </div>
          <div>
            <dt>权限标识</dt>
            <dd>{{ permissions.length }} 项</dd>
          </div>
          <div>
            <dt>本次登录</dt>
            <dd>{{ loginTime }}</dd>
          </div>
        </dl>
      </section>

      <!-- 快捷入口 -->
      <section class="card">
        <div class="card-head">
          <h3>快捷入口</h3>
          <span class="tag-line">按角色权限展示</span>
        </div>
        <div class="links" v-if="quickLinks.length">
          <button v-for="link in quickLinks" :key="link.path" type="button" @click="$router.push(link.path)">
            <span class="link-ico" v-html="link.icon"></span>
            <b>{{ link.name }}</b>
          </button>
        </div>
        <p class="empty" v-else>当前角色暂无可进入的管理页面，可在左侧菜单浏览商城。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import useInfo from '@/store/userInfo'
import stockHttp from '@/api/stockHttp'
import dashboardHttp from '@/api/dashboardHttp'

const userStore = useInfo()
const user = computed(() => userStore.userInfo.user || {})
const roles = computed(() => userStore.userInfo.roles || [])
const permissions = computed(() => userStore.userInfo.permissions || [])

const loginTime = new Date().toLocaleString('zh-CN', { hour12: false })
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const isMember = computed(() => roles.value.includes('member') && !roles.value.includes('super_admin'))

const heroTitle = computed(() => (isMember.value ? '今天想买点什么' : '今天也把琐事交给系统'))
const heroDesc = computed(() =>
  isMember.value
    ? '浏览商品、加入购物车、下单支付与售后评价，都在左侧菜单里。'
    : '补货建议、库存预警、客服应答、经营看板，都在左侧菜单里。'
)

const helloText = computed(() => {
  const name = user.value.nickName || user.value.username
  return name ? greeting.value + '，' + name : greeting.value
})

const userTypeText = computed(() => {
  const t = user.value.userType
  if (t === 0) return '运营'
  if (t === 1) return '消费者'
  return '-'
})

const ROLE_LABEL = {
  super_admin: '超级管理员',
  product_op: '商品运营',
  market_op: '活动运营',
  cs_op: '客服主管',
  member: '消费者'
}
const roleLabels = computed(() => roles.value.map(r => ROLE_LABEL[r] || r))

const isSuperAdmin = computed(() => roles.value.includes('super_admin'))
const hasPerm = perm => isSuperAdmin.value || permissions.value.includes(perm)

// 快捷入口：按权限过滤，避免跳到该角色没有注册的路由（会 404）
const allLinks = [
  { name: '数据看板', path: '/dashboard', perm: 'dashboard:view', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 19V9M12 19V4.5M19 19v-6" stroke-linecap="round"/><path d="M3.5 21h17" stroke-linecap="round"/></svg>' },
  { name: '商品列表', path: '/product/list', perm: 'product:item', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3.5 8.5 12 4l8.5 4.5v7L12 20l-8.5-4.5z"/><path d="M3.5 8.5 12 13l8.5-4.5M12 13v7"/></svg>' },
  { name: '库存预警', path: '/stock/alert', perm: 'stock:alert', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 4.5 21 19H3z" stroke-linejoin="round"/><path d="M12 10v4M12 16.6v.2" stroke-linecap="round"/></svg>' },
  { name: '客服中心', path: '/chat/knowledge', perm: 'chat:manage', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 6.5h16v9.5H9l-4 3.5v-3.5H4z" stroke-linejoin="round"/><path d="M8.5 10h7M8.5 12.8h4.5" stroke-linecap="round"/></svg>' },
  { name: '订单管理', path: '/order/manage', perm: 'order:manage', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4.5" y="3.5" width="15" height="17" rx="2.5"/><path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" stroke-linecap="round"/></svg>' },
  { name: '购物车', path: '/cart', perm: 'member:cart', memberOnly: true, icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3.5 4.5h2.2l2.1 10.2h9.7l2-7.4H6.4" stroke-linejoin="round"/><circle cx="9.5" cy="19" r="1.4"/><circle cx="17" cy="19" r="1.4"/></svg>' },
  { name: '我的订单', path: '/order/list', perm: 'member:order', memberOnly: true, icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4.5" y="3.5" width="15" height="17" rx="2.5"/><path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" stroke-linecap="round"/></svg>' },
  { name: '去逛商城', path: '/shop', perm: 'shop', memberOnly: true, icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4.5 8h15l-1.2 11.5H5.7z" stroke-linejoin="round"/><path d="M9 8V6.5a3 3 0 0 1 6 0V8" stroke-linecap="round"/></svg>' }
]
// 购物车/我的订单是消费者专属入口：超管虽有通配权限，但不该在运营台看到
const quickLinks = computed(() =>
  allLinks.filter(link => (link.memberOnly ? isMember.value : hasPerm(link.perm)))
)

// ---------- 今日经营概览 ----------
const realtime = ref(null)
const canSeeDashboard = computed(() => hasPerm('dashboard:view'))

const money = value => {
  const num = Number(value || 0)
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const momText = value => {
  if (value === null || value === undefined || value === '') return '环比 —'
  const num = Number(value)
  return '环比 ' + (num > 0 ? '+' : '') + num + '%'
}

const momTrend = value => {
  if (value === null || value === undefined || value === '') return 'flat'
  return Number(value) >= 0 ? 'up' : 'down'
}

const kpiList = computed(() => {
  const r = realtime.value
  if (!r) return []
  return [
    { label: '今日 GMV', value: money(r.gmv), mom: momText(r.gmvMoM), trend: momTrend(r.gmvMoM) },
    { label: '支付订单', value: Number(r.paidCount || 0), unit: '单', mom: momText(r.paidCountMoM), trend: momTrend(r.paidCountMoM) },
    { label: '客单价', value: money(r.avgAmount), mom: '', hint: 'GMV ÷ 支付订单' },
    { label: '访客数', value: Number(r.visitorCount || 0), unit: '人', mom: momText(r.visitorMoM), trend: momTrend(r.visitorMoM) },
    {
      label: '支付转化率',
      value: r.conversion === null || r.conversion === undefined ? '—' : r.conversion + '%',
      mom: '',
      hint: '支付订单 ÷ 访客'
    }
  ]
})

const alertCount = ref(0)
const canSeeAlert = computed(() => hasPerm('stock:alert'))

onMounted(async () => {
  if (canSeeDashboard.value) {
    try {
      const res = await dashboardHttp.getRealtime()
      realtime.value = res.data
    } catch (e) {
      realtime.value = null
    }
  }
  if (!canSeeAlert.value) return
  try {
    const res = await stockHttp.getUnhandledCount()
    alertCount.value = res.data || 0
  } catch (e) {
    alertCount.value = 0
  }
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ---------------- 顶部横幅 ---------------- */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  padding: 26px 30px;
  color: #fff;
  background: linear-gradient(140deg, #08142c 0%, #10275c 55%, #1b4fa8 100%);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  box-shadow: 0 18px 34px -24px rgba(11, 30, 70, 0.9);
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(ellipse at 20% 10%, #000 15%, transparent 72%);
  -webkit-mask-image: radial-gradient(ellipse at 20% 10%, #000 15%, transparent 72%);
}

.hero-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  right: -70px;
  top: -120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(88, 160, 255, 0.75), transparent 68%);
  filter: blur(50px);
}

.hero-main {
  position: relative;
  z-index: 1;
}

.hello {
  margin: 0 0 8px;
  font-size: 12.5px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.62);
}

.hero-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.6px;
}

.hero-desc {
  margin: 0 0 16px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.55);
}

.pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill {
  font-size: 11.5px;
  padding: 3px 10px;
  border-radius: 999px;
  color: #0b1a3c;
  background: linear-gradient(140deg, #cfe3ff, #9dc4ff);
  font-weight: 600;
}

.pill.ghost {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.78);
  font-weight: 400;
}

.hero-side {
  position: relative;
  z-index: 1;
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 0 0 auto;
}

.live {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  font-size: 12px;
  color: #9ff0c2;
}

.live i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.6);
  animation: pulse 2s infinite;
}

.since {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.45);
}

@keyframes pulse {
  70% { box-shadow: 0 0 0 7px rgba(52, 211, 153, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0); }
}

/* ---------------- 预警条 ---------------- */
.alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.alert-ico {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d97706;
  background: #fef3c7;
}

.alert-ico :deep(svg) {
  width: 18px;
  height: 18px;
}

.alert-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.alert-text b {
  font-size: 13px;
  color: #92400e;
}

.alert-text span {
  font-size: 12px;
  color: #b45309;
}

.alert-btn {
  border-radius: 9px;
  border-color: #fcd34d;
  color: #92400e;
  background: #fff;
}

.alert-btn:hover {
  background: #fef3c7;
  border-color: #fbbf24;
  color: #92400e;
}

/* ---------------- 卡片通用 ---------------- */
.row2 {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 14px;
}

.card {
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 16px;
  padding: 20px 22px;
  box-shadow: 0 8px 20px -18px rgba(15, 23, 42, 0.5);
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-head h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 0.3px;
}

.tag-line {
  font-size: 11.5px;
  color: #b0b8c4;
}

/* 今日经营 */
.today .card-head {
  margin-bottom: 14px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
}

.live-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: #059669;
}

.live-dot i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.55);
  animation: pulse 2s infinite;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.kpi {
  padding: 13px 15px;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  background: linear-gradient(180deg, #fbfcfe, #f8fafd);
  transition: all 0.18s ease;
}

.kpi:hover {
  background: #fff;
  border-color: #dbeafe;
  transform: translateY(-2px);
  box-shadow: 0 14px 24px -20px rgba(15, 23, 42, 0.7);
}

.kpi-label {
  display: block;
  font-size: 11.5px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.kpi-value {
  display: block;
  font-size: 21px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.3px;
  font-variant-numeric: tabular-nums;
  line-height: 1.25;
}

.kpi-value em {
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  margin-left: 3px;
}

.mom {
  display: inline-block;
  margin-top: 6px;
  font-size: 11.5px;
}

.mom.up {
  color: #059669;
}

.mom.down {
  color: #dc2626;
}

.mom.flat,
.mom.hint {
  color: #b0b8c4;
}

.kpi-foot {
  margin: 12px 0 0;
  font-size: 11.5px;
  color: #b0b8c4;
}

/* 账号信息 */
.info {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 10px;
}

.info dt {
  font-size: 11.5px;
  color: #94a3b8;
  margin-bottom: 3px;
}

.info dd {
  margin: 0;
  font-size: 13.5px;
  color: #1f2937;
  word-break: break-all;
}

.role {
  display: inline-block;
  font-size: 11.5px;
  padding: 2px 9px;
  margin: 0 6px 4px 0;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.muted {
  color: #c3cad6;
}

/* 快捷入口 */
.links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.links button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 13px;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  background: #fbfcfe;
  cursor: pointer;
  transition: all 0.18s ease;
  text-align: left;
}

.links button:hover {
  border-color: #bfdbfe;
  background: #f5f9ff;
  transform: translateY(-2px);
  box-shadow: 0 12px 22px -18px rgba(37, 99, 235, 0.8);
}

.link-ico {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  background: #eff6ff;
}

.link-ico :deep(svg) {
  width: 16px;
  height: 16px;
}

.links b {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.empty {
  margin: 0;
  font-size: 12.5px;
  color: #94a3b8;
  line-height: 1.8;
}

@media (max-width: 1180px) {
  .row2 {
    grid-template-columns: 1fr;
  }

  .kpis {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 720px) {
  .hero {
    flex-direction: column;
  }

  .hero-side {
    text-align: left;
    flex-direction: row;
    gap: 12px;
  }

  .links {
    grid-template-columns: 1fr 1fr;
  }

  .kpis {
    grid-template-columns: 1fr 1fr;
  }

  .info {
    grid-template-columns: 1fr;
  }
}
</style>
