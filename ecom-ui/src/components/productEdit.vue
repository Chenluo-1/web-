<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>{{ isEdit ? '编辑商品' : '新增商品' }}</span>
          <el-button @click="$router.push('/product/list')">返回列表</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="base-form">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="请输入商品标题" />
        </el-form-item>
        <el-form-item label="类目" prop="categoryId">
          <el-tree-select
            v-model="form.categoryId"
            :data="tree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="请选择三级类目（商品必须挂三级）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :http-request="doUpload"
            :on-success="onUploadSuccess"
            accept="image/*"
            multiple
          >
            <span class="upload-plus">+</span>
          </el-upload>
          <div class="tip">第一张作为列表主图（jpg/png/gif/webp，单张 ≤10MB）</div>
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="商品描述（富文本可后续扩展）" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">规格与 SKU（笛卡尔积自动生成）</el-divider>

      <div v-for="(spec, si) in specs" :key="si" class="spec-row">
        <el-input v-model="spec.specName" placeholder="规格名（如 颜色）" style="width: 160px" />
        <span class="colon">：</span>
        <div class="values">
          <el-tag v-for="(val, vi) in spec.values" :key="vi" closable @close="removeValue(si, vi)">{{ val }}</el-tag>
          <el-input
            size="small"
            style="width: 150px"
            placeholder="输入值后回车添加"
            @keyup.enter="addValue(si, $event)"
          />
        </div>
        <el-button link type="danger" @click="removeSpec(si)">删除维度</el-button>
      </div>

      <div class="spec-actions">
        <el-button :disabled="specs.length >= 3" @click="addSpec">添加规格维度</el-button>
        <el-button type="primary" plain @click="regenerateSkus">重新生成 SKU 组合</el-button>
        <span class="tip">增加/删除规格值会自动重算组合，已填的价格库存按「规格组合」保留</span>
      </div>

      <el-table v-if="skuRows.length" :data="skuRows" border class="sku-table">
        <el-table-column v-for="dim in activeDims" :key="dim.specName" :label="dim.specName" width="120">
          <template #default="{ row }">{{ row.specs[dim.specName] }}</template>
        </el-table-column>
        <el-table-column label="价格(¥)" width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="1" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :step="1" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="SKU 编码" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.skuCode" size="small" placeholder="可选" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="先添加规格维度和值，SKU 组合会自动生成" :image-size="70" />

      <div class="submit-bar">
        <el-button type="primary" size="large" @click="submit">保存商品</el-button>
        <span class="tip">新商品默认下架；所有 SKU 库存为 0 时会自动下架</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import productHttp from '@/api/productHttp'

const route = useRoute()
const router = useRouter()

const isEdit = ref(false)
const tree = ref([])
const formRef = ref(null)
const form = reactive({ id: null, title: '', categoryId: null, description: '', mainImage: null })
const rules = {
  title: [{ required: true, message: '商品标题不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }]
}

const fileList = ref([])

// ---------- 图片上传 ----------
const doUpload = async param => {
  const fd = new FormData()
  fd.append('file', param.file)
  return productHttp.uploadFile(fd)
}

const onUploadSuccess = (response, file) => {
  file.url = response.data.url
}

// ---------- 规格 ----------
const specs = ref([])
const skuRows = ref([])

const activeDims = computed(() => specs.value.filter(s => s.specName && s.values.length))

const comboKey = obj =>
  JSON.stringify(Object.keys(obj).sort().map(k => [k, obj[k]]))

const regenerateSkus = () => {
  const dims = activeDims.value
  if (!dims.length) {
    skuRows.value = []
    return
  }
  const combos = dims.reduce(
    (acc, dim) => acc.flatMap(row => dim.values.map(v => ({ ...row, [dim.specName]: v }))),
    [{}]
  )
  const oldMap = new Map()
  skuRows.value.forEach(r => oldMap.set(comboKey(r.specs), r))
  skuRows.value = combos.map(combo => {
    const old = oldMap.get(comboKey(combo))
    return old ? old : { id: null, specs: combo, skuCode: '', price: null, stock: 0 }
  })
}

const addValue = (si, event) => {
  const val = (event.target.value || '').trim()
  if (!val) return
  if (specs.value[si].values.includes(val)) {
    ElMessage.warning('该值已存在')
  } else {
    specs.value[si].values.push(val)
    regenerateSkus()
  }
  event.target.value = ''
}

const removeValue = (si, vi) => {
  specs.value[si].values.splice(vi, 1)
  regenerateSkus()
}

const addSpec = () => {
  specs.value.push({ specName: '', values: [] })
}

const removeSpec = si => {
  specs.value.splice(si, 1)
  regenerateSkus()
}

// ---------- 保存 ----------
const submit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    const dims = activeDims.value
    if (!dims.length) {
      ElMessage.warning('请至少添加一个规格维度及其可选值')
      return
    }
    if (skuRows.value.some(r => r.price === null || r.price === undefined)) {
      ElMessage.warning('请填写所有 SKU 的价格')
      return
    }
    form.mainImage = fileList.value.length ? (fileList.value[0].url || null) : null
    const payload = {
      ...form,
      specs: dims.map(s => ({ specName: s.specName, values: s.values })),
      skus: skuRows.value.map(r => ({
        id: r.id, specs: r.specs, skuCode: r.skuCode, price: r.price, stock: r.stock
      })),
      images: fileList.value.map(f => f.url).filter(Boolean)
    }
    if (isEdit.value) {
      await productHttp.updateProduct(payload)
    } else {
      await productHttp.addProduct(payload)
    }
    ElMessage.success('保存成功')
    router.push('/product/list')
  })
}

// ---------- 初始加载 ----------
const loadTree = async () => {
  const res = await productHttp.selectCategoryTree()
  tree.value = res.data
}

onMounted(async () => {
  loadTree()
  const id = route.query.id
  if (!id) return
  isEdit.value = true
  const res = await productHttp.getProductById(id)
  const d = res.data
  Object.assign(form, {
    id: d.product.id,
    title: d.product.title,
    categoryId: d.product.categoryId,
    description: d.product.description,
    mainImage: d.product.mainImage
  })
  specs.value = d.specs.map(s => ({ specName: s.specName, values: JSON.parse(s.specValues || '[]') }))
  skuRows.value = d.skus.map(s => ({
    id: s.id, specs: JSON.parse(s.specs || '{}'), skuCode: s.skuCode, price: s.price, stock: s.stock
  }))
  fileList.value = d.images.map(i => ({ name: i.url.split('/').pop(), url: i.url }))
})
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.base-form {
  max-width: 760px;
}

.tip {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.upload-plus {
  font-size: 24px;
  color: #8c939d;
}

.spec-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.colon {
  margin: 0 6px;
  color: #606266;
}

.values {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.spec-actions {
  display: flex;
  align-items: center;
  margin: 14px 0;
}

.sku-table {
  margin-top: 8px;
}

.submit-bar {
  margin-top: 20px;
  display: flex;
  align-items: center;
}
</style>
