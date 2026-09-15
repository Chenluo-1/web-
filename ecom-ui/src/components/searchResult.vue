<template>
  <div>
    <el-card shadow="never" class="top-card">
      <div class="bar">
        <search-bar />
        <span class="kw-info" v-if="keyword">关键词：「{{ keyword }}」</span>
      </div>
      <el-alert v-if="result && result.hint" :title="result.hint" type="info" :closable="false" class="hint" />
    </el-card>

    <!-- 结果列表 -->
    <el-row :gutter="14" class="grid" v-loading="loading">
      <el-col :span="6" v-for="item in (result ? result.records : [])" :key="item.id">
        <el-card shadow="hover" class="product-card">
          <el-image :src="item.mainImage" fit="cover" class="product-img">
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

    <!-- 无结果推荐 -->
    <template v-if="result && !result.records.length">
      <el-divider content-position="left">为你推荐</el-divider>
      <el-row :gutter="14">
        <el-col :span="6" v-for="item in result.recommend" :key="'r-' + item.id">
          <el-card shadow="hover" class="product-card">
            <el-image :src="item.mainImage" fit="cover" class="product-img">
              <template #error><div class="img-holder">暂无图片</div></template>
            </el-image>
            <div class="product-title">{{ item.title }}</div>
            <div class="product-price"><span class="price">¥{{ item.priceMin }}</span></div>
            <el-button type="primary" class="add-btn" @click="openSkuDialog(item)">加入购物车</el-button>
          </el-card>
        </el-col>
      </el-row>
    </template>

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
        <div class="qty">数量：<el-input-number v-model="quantity" :min="1" :max="matchedSku ? matchedSku.stock : 1" /></div>
      </template>
      <template #footer>
        <el-button @click="skuVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!matchedSku" @click="confirmAdd">加入购物车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import searchHttp from '@/api/searchHttp'
import productHttp from '@/api/productHttp'
import cartHttp from '@/api/cartHttp'
import behaviorHttp from '@/api/behaviorHttp'
import SearchBar from '@/components/searchBar.vue'

const route = useRoute()
const loading = ref(false)
const result = ref(null)
const keyword = computed(() => route.query.keyword || '')

const doSearch = async () => {
  if (!keyword.value) return
  loading.value = true
  try {
    const res = await searchHttp.search({ keyword: keyword.value, pageNum: 1, pageSize: 12 })
    result.value = res.data
  } finally {
    loading.value = false
  }
}

watch(keyword, doSearch)
onMounted(doSearch)

// ---------- SKU 选择加购 ----------
const skuVisible = ref(false)
const currentProduct = ref(null)
const specDims = ref([])
const selected = reactive({})
const quantity = ref(1)

const matchedSku = computed(() => {
  if (!currentProduct.value) return null
  if (specDims.value.some(d => !selected[d.specName])) return null
  return currentProduct.value.skus.find(s =>
    specDims.value.every(d => s.specsMap[d.specName] === selected[d.specName])
  ) || null
})

const openSkuDialog = async item => {
  behaviorHttp.report({ eventType: 'expose', targetType: 'product', targetId: item.id }).catch(() => {})
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
</script>

<style scoped>
.top-card {
  margin-bottom: 14px;
}

.bar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.kw-info {
  color: #909399;
}

.hint {
  margin-top: 10px;
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
</style>
