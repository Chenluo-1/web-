<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>我的购物车</span>
          <el-button link type="danger" :disabled="!list.length" @click="handleClearAll">清空购物车</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading">
        <el-table-column width="50">
          <template #default="{ row }">
            <el-checkbox
              :model-value="row.checked === 1"
              @change="val => toggleChecked(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="row.image" fit="cover" class="thumb">
                <template #error><div class="thumb-holder">无图</div></template>
              </el-image>
              <div>
                <div class="title">{{ row.title }}</div>
                <div class="spec">{{ formatSpecs(row.specs) }}</div>
                <el-tag v-if="row.productStatus !== 1" type="info" size="small">已下架</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number
              :model-value="row.quantity"
              :min="1"
              :max="Math.max(row.stock, 1)"
              size="small"
              @change="val => changeQuantity(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="110">
          <template #default="{ row }">
            <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="购物车还是空的，去逛逛吧" />

      <div class="footer" v-if="list.length">
        <el-checkbox
          :model-value="allChecked"
          :indeterminate="someChecked && !allChecked"
          @change="toggleAll"
        >全选</el-checkbox>
        <div class="total-area">
          <span>已选 <b>{{ checkedCount }}</b> 件，合计：</span>
          <span class="total">¥{{ checkedTotal.toFixed(2) }}</span>
          <el-button type="danger" size="large" :disabled="!checkedCount" @click="goConfirm">去结算</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import cartHttp from '@/api/cartHttp'

const router = useRouter()
const loading = ref(false)
const list = ref([])

const loadCart = async () => {
  loading.value = true
  try {
    const res = await cartHttp.selectMyCart()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const formatSpecs = specs => {
  try {
    const map = JSON.parse(specs || '{}')
    return Object.keys(map).map(k => k + ': ' + map[k]).join(' / ')
  } catch (e) {
    return specs
  }
}

const checkedList = computed(() => list.value.filter(i => i.checked === 1))
const checkedCount = computed(() => checkedList.value.reduce((s, i) => s + i.quantity, 0))
const checkedTotal = computed(() => checkedList.value.reduce((s, i) => s + Number(i.price) * i.quantity, 0))
const allChecked = computed(() => list.value.length > 0 && checkedList.value.length === list.value.length)
const someChecked = computed(() => checkedList.value.length > 0)

const toggleChecked = async (row, val) => {
  await cartHttp.updateCartChecked(row.id, val ? 1 : 0)
  loadCart()
}

const toggleAll = async val => {
  await cartHttp.checkAllCart(val ? 1 : 0)
  loadCart()
}

const changeQuantity = async (row, val) => {
  if (!val) return
  await cartHttp.updateCartQuantity(row.id, val)
  loadCart()
}

const handleDelete = row => {
  ElMessageBox.confirm(`确定从购物车删除「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await cartHttp.delCart([row.id])
      ElMessage.success('已删除')
      loadCart()
    })
    .catch(() => {})
}

const handleClearAll = () => {
  ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
    .then(async () => {
      await cartHttp.delCart(list.value.map(i => i.id))
      loadCart()
    })
    .catch(() => {})
}

const goConfirm = () => {
  const bad = checkedList.value.some(i => i.productStatus !== 1)
  if (bad) {
    ElMessage.warning('选中商品中存在已下架商品，请移除后再结算')
    return
  }
  router.push('/order/confirm')
}

onMounted(loadCart)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.thumb-holder {
  width: 64px;
  height: 64px;
  line-height: 64px;
  text-align: center;
  color: #c0c4cc;
  background: #f5f7fa;
  font-size: 12px;
}

.title {
  color: #303133;
}

.spec {
  color: #909399;
  font-size: 12px;
  margin: 3px 0;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.total-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}
</style>
