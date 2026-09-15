<template>
  <div>
    <el-card shadow="never" class="toolbar-card">
      <div class="bar">
        <search-bar />
        <el-button type="warning" plain @click="openCouponDrawer">领券中心</el-button>
      </div>
    </el-card>

    <el-row :gutter="14" class="grid" v-loading="loading">
      <el-col :span="6" v-for="item in list" :key="item.id">
        <el-card shadow="hover" class="product-card" @click="reportClick(item.id)">
          <el-image :src="item.mainImage" fit="cover" class="product-img" :preview-src-list="[item.mainImage]" preview-teleported>
            <template #error><div class="img-holder">暂无图片</div></template>
          </el-image>
          <div class="product-title">{{ item.title }}</div>
          <div class="product-price">
            <span class="price">¥{{ item.priceMin }}<template v-if="item.priceMax != item.priceMin"> ~ ¥{{ item.priceMax }}</template></span>
            <span class="sales">已售 {{ item.sales }}</span>
          </div>
          <el-button type="primary" class="add-btn" @click="openSkuDialog(item)">加入购物车</el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !list.length" description="暂无上架商品" />

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.pageNum"
      :page-size="query.pageSize"
      @current-change="loadPage"
    />

    <!-- SKU 选择 -->
    <el-dialog v-model="skuVisible" :title="currentProduct ? currentProduct.title : '选择规格'" width="520px">
      <template v-if="currentProduct">
        <div v-for="dim in specDims" :key="dim.specName" class="spec-dim">
          <span class="dim-name">{{ dim.specName }}：</span>
          <el-radio-group v-model="selected[dim.specName]" size="small">
            <el-radio-button v-for="v in dim.values" :key="v" :value="v">{{ v }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="sku-info">
          <template v-if="matchedSku">
            <span class="price">¥{{ matchedSku.price }}</span>
            <span class="stock">库存 {{ matchedSku.stock }} 件</span>
          </template>
          <el-tag v-else type="warning">该组合暂无货</el-tag>
        </div>
        <div class="qty">
          数量：<el-input-number v-model="quantity" :min="1" :max="matchedSku ? matchedSku.stock : 1" />
        </div>

        <div class="review-block">
          <div class="rv-head">
            <span>商品评价（{{ reviews.total }}）</span>
            <el-rate :model-value="reviewAvg" disabled show-score text-color="#ff9900" size="small" />
          </div>
          <div v-for="r in reviews.records" :key="r.id" class="rv-row">
            <div class="rv-top">
              <span class="rv-user">{{ r.nickName || '匿名用户' }}</span>
              <el-rate :model-value="r.rating" disabled size="small" />
              <span class="rv-time">{{ formatDate(r.createTime) }}</span>
            </div>
            <div class="rv-content">{{ r.content || '此用户没有填写评价内容' }}</div>
          </div>
          <div v-if="!reviews.records.length" class="rv-empty">暂无评价</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="skuVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!matchedSku" @click="confirmAdd">加入购物车</el-button>
      </template>
    </el-dialog>

    <!-- 领券中心 -->
    <el-dialog v-model="couponVisible" title="领券中心" width="460px">
      <div v-for="c in availableCoupons" :key="c.id" class="coupon-row">
        <div>
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-cond">满 {{ c.thresholdAmount }} 元可用 ｜ 有效期至 {{ formatDate(c.endTime) }}</div>
        </div>
        <el-button type="warning" size="small" @click="receive(c)">领取</el-button>
      </div>
      <el-empty v-if="!availableCoupons.length" description="暂无可领取优惠券" :image-size="70" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import productHttp from '@/api/productHttp'
import cartHttp from '@/api/cartHttp'
import couponHttp from '@/api/couponHttp'
import behaviorHttp from '@/api/behaviorHttp'
import reviewHttp from '@/api/reviewHttp'
import SearchBar from '@/components/searchBar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 8, title: '', status: 1 })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await productHttp.selectProductByPage(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  loadPage()
}

// ---------- SKU 选择加购 ----------
const skuVisible = ref(false)
const currentProduct = ref(null)
const specDims = ref([])
const selected = reactive({})
const quantity = ref(1)

const matchedSku = computed(() => {
  if (!currentProduct.value) return null
  const chosen = specDims.value.map(d => selected[d.specName])
  if (chosen.some(v => !v)) return null
  return currentProduct.value.skus.find(s => {
    const specs = s.specsMap
    return specDims.value.every(d => specs[d.specName] === selected[d.specName])
  }) || null
})

const reportClick = id => {
  behaviorHttp.report({ eventType: 'click', targetType: 'product', targetId: id }).catch(() => {})
}

// ---------- 商品评价（简化版：均分 + 最新 3 条） ----------
const reviews = reactive({ total: 0, records: [] })
const reviewAvg = ref(0)

const loadReviews = async id => {
  reviews.total = 0
  reviews.records = []
  reviewAvg.value = 0
  try {
    const [page, avg] = await Promise.all([
      reviewHttp.selectByProduct(id, { pageNum: 1, pageSize: 3 }),
      reviewHttp.selectAvgRating(id)
    ])
    reviews.total = page.data.total
    reviews.records = page.data.records
    reviewAvg.value = avg.data
  } catch (e) {
    // 评价加载失败不影响加购主流程
  }
}

const openSkuDialog = async item => {
  behaviorHttp.report({ eventType: 'expose', targetType: 'product', targetId: item.id }).catch(() => {})
  loadReviews(item.id)
  const res = await productHttp.getProductById(item.id)
  const d = res.data
  currentProduct.value = {
    id: d.product.id,
    title: d.product.title,
    skus: d.skus.map(s => ({ ...s, specsMap: JSON.parse(s.specs || '{}') }))
  }
  specDims.value = d.specs.map(s => ({ specName: s.specName, values: JSON.parse(s.specValues || '[]') }))
  specDims.value.forEach(dim => { selected[dim.specName] = dim.values[0] || null })
  quantity.value = 1
  skuVisible.value = true
}

const confirmAdd = async () => {
  if (!matchedSku.value) return
  await cartHttp.addCart({ skuId: matchedSku.value.id, quantity: quantity.value })
  ElMessage.success('已加入购物车')
  skuVisible.value = false
}

// ---------- 领券 ----------
const couponVisible = ref(false)
const availableCoupons = ref([])

const openCouponDrawer = async () => {
  const res = await couponHttp.selectAvailableCoupon()
  availableCoupons.value = res.data
  couponVisible.value = true
}

const receive = async c => {
  await couponHttp.receiveCoupon(c.id)
  ElMessage.success('领取成功')
}

const formatDate = v => (v ? String(v).substring(0, 10) : '')

onMounted(loadPage)
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 14px;
}

.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-card {
  margin-bottom: 14px;
}

.product-img {
  width: 100%;
  height: 160px;
}

.img-holder {
  width: 100%;
  height: 160px;
  line-height: 160px;
  text-align: center;
  color: #c0c4cc;
  background: #f5f7fa;
}

.product-title {
  margin-top: 8px;
  height: 40px;
  overflow: hidden;
  color: #303133;
}

.product-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}

.sales {
  color: #909399;
  font-size: 12px;
}

.add-btn {
  width: 100%;
}

.pager {
  margin-top: 6px;
  justify-content: flex-end;
}

.spec-dim {
  margin-bottom: 12px;
}

.dim-name {
  color: #606266;
}

.sku-info {
  margin: 10px 0;
}

.stock {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.qty {
  margin-top: 8px;
  color: #606266;
}

.coupon-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #f3d19e;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.coupon-name {
  font-weight: bold;
}

.coupon-cond {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.review-block {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.rv-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
}

.rv-row {
  padding: 6px 0;
  border-bottom: 1px dashed #f5f7fa;
}

.rv-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rv-user {
  color: #409eff;
  font-size: 13px;
}

.rv-time {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: auto;
}

.rv-content {
  color: #606266;
  font-size: 13px;
  margin-top: 2px;
}

.rv-empty {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 6px 0;
}
</style>
