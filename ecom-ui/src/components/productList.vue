<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="商品名称">
          <el-input v-model="query.title" placeholder="商品名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类目">
          <el-tree-select
            v-model="query.categoryId"
            :data="tree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="全部类目"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" v-hasPermi="'product:item:find'" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" v-hasPermi="'product:item:add'" @click="goEdit()">新增商品</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column label="主图" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.mainImage"
              :src="row.mainImage"
              fit="cover"
              style="width: 60px; height: 60px"
              :preview-src-list="[row.mainImage]"
              preview-teleported
            />
            <span v-else class="no-img">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" min-width="180" />
        <el-table-column label="类目" min-width="120">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="价格区间" width="140">
          <template #default="{ row }">¥{{ row.priceMin }} ~ ¥{{ row.priceMax }}</template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="上下架" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="!hasPermi('product:item:status')"
              @change="val => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="'product:item:edit'" @click="goEdit(row.id)">编辑</el-button>
            <el-button link type="danger" v-hasPermi="'product:item:del'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="loadPage"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import productHttp from '@/api/productHttp'
import useInfo from '@/store/userInfo'

const router = useRouter()
const userStore = useInfo()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const tree = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, title: '', categoryId: null, status: null })

const hasPermi = code => {
  const roles = userStore.userInfo.roles || []
  const perms = userStore.userInfo.permissions || []
  return roles.includes('super_admin') || perms.includes(code)
}

const categoryName = id => {
  let found = null
  const walk = list => {
    for (const c of list) {
      if (c.id === id) { found = c.name; return }
      if (c.children && c.children.length) walk(c.children)
    }
  }
  walk(tree.value)
  return found || '-'
}

const loadPage = async () => {
  loading.value = true
  try {
    const res = await productHttp.selectProductByPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  const res = await productHttp.selectCategoryTree()
  tree.value = res.data
}

const handleSearch = () => {
  query.pageNum = 1
  loadPage()
}

const resetQuery = () => {
  query.title = ''
  query.categoryId = null
  query.status = null
  handleSearch()
}

const handleSizeChange = () => {
  query.pageNum = 1
  loadPage()
}

const goEdit = id => {
  router.push(id ? `/product/edit?id=${id}` : '/product/edit')
}

const toggleStatus = row => {
  const target = row.status === 1 ? 0 : 1
  const action = target === 1 ? '上架' : '下架'
  ElMessageBox.confirm(`确定${action}商品「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await productHttp.updateProductStatus(row.id, target)
      ElMessage.success(action + '成功')
      loadPage()
    })
    .catch(() => {})
}

const handleDelete = row => {
  ElMessageBox.confirm(`确定删除商品「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await productHttp.delProduct([row.id])
      ElMessage.success('删除成功')
      loadPage()
    })
    .catch(() => {})
}

onMounted(() => {
  loadTree()
  loadPage()
})
</script>

<style scoped>
.table-card {
  margin-top: 16px;
}

.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.no-img {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
