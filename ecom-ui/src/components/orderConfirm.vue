<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head"><span>确认订单</span></div>
      </template>

      <!-- 收货地址 -->
      <el-divider content-position="left">收货地址</el-divider>
      <div v-if="addresses.length" class="addr-list">
        <div
          v-for="a in addresses"
          :key="a.id"
          class="addr-item"
          :class="{ active: selectedAddressId === a.id }"
          @click="selectedAddressId = a.id"
        >
          <div class="addr-top">
            <b>{{ a.receiverName }}</b>
            <span class="addr-phone">{{ a.phone }}</span>
            <el-tag v-if="a.isDefault === 1" size="small" type="success">默认</el-tag>
          </div>
          <div class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detail }}</div>
        </div>
      </div>
      <el-empty v-else description="还没有收货地址，请先新增" :image-size="70" />
      <el-button class="addr-add" size="small" @click="openAddrDialog()">新增地址</el-button>

      <!-- 商品清单 -->
      <el-divider content-position="left">商品清单</el-divider>
      <el-table :data="items" size="small">
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image :src="row.image" fit="cover" class="thumb">
                <template #error><div class="thumb-holder">无图</div></template>
              </el-image>
              <div>
                <div>{{ row.title }}</div>
                <div class="spec">{{ formatSpecs(row.specs) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="70" prop="quantity" />
        <el-table-column label="小计" width="110">
          <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <!-- 优惠券 + 备注 -->
      <el-divider content-position="left">优惠与备注</el-divider>
      <el-form label-width="90px" class="confirm-form">
        <el-form-item label="优惠券">
          <el-select v-model="selectedUserCouponId" placeholder="不使用优惠券" clearable style="width: 320px">
            <el-option
              v-for="c in usableCoupons"
              :key="c.userCouponId"
              :label="`${c.name}（满 ${c.thresholdAmount} 可用）`"
              :value="c.userCouponId"
            />
          </el-select>
          <span class="tip">共 {{ myCoupons.length }} 张可用券</span>
        </el-form-item>
        <el-form-item label="订单备注">
          <el-input v-model="remark" placeholder="选填" maxlength="100" style="width: 320px" />
        </el-form-item>
      </el-form>

      <!-- 金额 -->
      <div class="amount-bar">
        <div>商品总额：<b>¥{{ totalAmount.toFixed(2) }}</b></div>
        <div>优惠金额：<b class="discount">-¥{{ discountAmount.toFixed(2) }}</b></div>
        <div class="pay">应付：<span class="pay-num">¥{{ payAmount.toFixed(2) }}</span></div>
        <el-button type="danger" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </el-card>

    <!-- 新增地址 -->
    <el-dialog v-model="addrVisible" title="新增收货地址" width="480px">
      <el-form ref="addrFormRef" :model="addrForm" :rules="addrRules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName"><el-input v-model="addrForm.receiverName" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="addrForm.phone" /></el-form-item>
        <el-form-item label="省" prop="province"><el-input v-model="addrForm.province" /></el-form-item>
        <el-form-item label="市" prop="city"><el-input v-model="addrForm.city" /></el-form-item>
        <el-form-item label="区/县" prop="district"><el-input v-model="addrForm.district" /></el-form-item>
        <el-form-item label="详细地址" prop="detail"><el-input v-model="addrForm.detail" /></el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import cartHttp from '@/api/cartHttp'
import addressHttp from '@/api/addressHttp'
import couponHttp from '@/api/couponHttp'
import orderHttp from '@/api/orderHttp'

const router = useRouter()

const items = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const myCoupons = ref([])
const selectedUserCouponId = ref(null)
const remark = ref('')
const submitting = ref(false)

// 幂等键：进入确认页生成一次，重复点击不产生第二笔订单
const idempotencyKey = 'ECI' + Date.now() + Math.random().toString(36).slice(2, 10)

const totalAmount = computed(() => items.value.reduce((s, i) => s + Number(i.price) * i.quantity, 0))

const usableCoupons = computed(() =>
  myCoupons.value.filter(c => totalAmount.value >= Number(c.thresholdAmount))
)

const discountAmount = computed(() => {
  const c = usableCoupons.value.find(c => c.userCouponId === selectedUserCouponId.value)
  if (!c) return 0
  let d
  if (c.type === 1 && c.discountRate) {
    d = totalAmount.value * (1 - Number(c.discountRate))
  } else {
    d = Number(c.discountAmount || 0)
  }
  return Math.min(Math.round(d * 100) / 100, totalAmount.value)
})

const payAmount = computed(() => Math.max(totalAmount.value - discountAmount.value, 0))

const formatSpecs = specs => {
  try {
    const map = JSON.parse(specs || '{}')
    return Object.keys(map).map(k => k + ': ' + map[k]).join(' / ')
  } catch (e) {
    return specs
  }
}

const loadData = async () => {
  const [cartRes, addrRes, couponRes] = await Promise.all([
    cartHttp.selectMyCart(),
    addressHttp.selectMyAddress(),
    couponHttp.selectMyCoupon()
  ])
  items.value = cartRes.data.filter(i => i.checked === 1)
  addresses.value = addrRes.data
  myCoupons.value = couponRes.data
  const def = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
  selectedAddressId.value = def ? def.id : null
  if (!items.value.length) {
    ElMessage.warning('没有勾选的商品')
    router.replace('/cart')
  }
}

// ---------- 地址 ----------
const addrVisible = ref(false)
const addrFormRef = ref(null)
const addrForm = reactive({ receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
const addrRules = {
  receiverName: [{ required: true, message: '不能为空', trigger: 'blur' }],
  phone: [{ required: true, message: '不能为空', trigger: 'blur' }],
  detail: [{ required: true, message: '不能为空', trigger: 'blur' }]
}

const openAddrDialog = () => {
  Object.assign(addrForm, { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: addresses.value.length ? 0 : 1 })
  addrVisible.value = true
}

const saveAddress = () => {
  addrFormRef.value.validate(async valid => {
    if (!valid) return
    await addressHttp.addAddress(addrForm)
    ElMessage.success('地址已保存')
    addrVisible.value = false
    const res = await addressHttp.selectMyAddress()
    addresses.value = res.data
    const newest = addresses.value[0]
    if (newest) selectedAddressId.value = newest.id
  })
}

// ---------- 提交 ----------
const submitOrder = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const res = await orderHttp.createOrder({
      addressId: selectedAddressId.value,
      userCouponId: selectedUserCouponId.value,
      idempotencyKey,
      remark: remark.value
    })
    const orderNo = res.data.order.orderNo
    ElMessage.success('订单已创建，请尽快支付')
    router.replace('/order/pay?orderNo=' + orderNo)
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.addr-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.addr-item {
  width: 250px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
}

.addr-item.active {
  border-color: #f56c6c;
  background: #fef0f0;
}

.addr-phone {
  margin-left: 10px;
  color: #909399;
}

.addr-detail {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
}

.addr-add {
  margin-top: 10px;
}

.item-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.thumb {
  width: 50px;
  height: 50px;
}

.thumb-holder {
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #c0c4cc;
  background: #f5f7fa;
  font-size: 12px;
}

.spec {
  color: #909399;
  font-size: 12px;
}

.confirm-form {
  margin-top: 6px;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.amount-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 26px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.discount {
  color: #67c23a;
}

.pay-num {
  color: #f56c6c;
  font-size: 22px;
  font-weight: bold;
}
</style>
