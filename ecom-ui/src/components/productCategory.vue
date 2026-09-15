<template>
  <el-row :gutter="16">
    <!-- 左：类目树 -->
    <el-col :span="9">
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>类目树（最多三级）</span>
            <el-button type="primary" size="small" v-hasPermi="'product:category:add'" @click="startAddRoot">
              新增一级类目
            </el-button>
          </div>
        </template>
        <el-tree
          :data="tree"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="onSelect"
        >
          <template #default="{ data }">
            <span>
              {{ data.name }}
              <el-tag v-if="data.status === 1" size="small" type="info">停用</el-tag>
            </span>
          </template>
        </el-tree>
      </el-card>
    </el-col>

    <!-- 右：编辑表单 -->
    <el-col :span="15">
      <el-card shadow="never">
        <template #header>{{ formTitle }}</template>
        <el-empty v-if="mode === ''" description="点击左侧类目编辑，或点「新增一级类目」" />
        <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="90px" class="cat-form">
          <el-form-item label="类目名称" prop="name">
            <el-input v-model="form.name" maxlength="50" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="0" :inactive-value="1" active-text="正常" inactive-text="停用" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submit">
              {{ mode === 'edit' ? '保存修改' : '确定新增' }}
            </el-button>
            <el-button
              v-if="mode === 'edit' && current && current.level < 3"
              v-hasPermi="'product:category:add'"
              @click="startAddChild"
            >新增子类目</el-button>
            <el-button
              v-if="mode === 'edit'"
              type="danger"
              v-hasPermi="'product:category:del'"
              @click="handleDelete"
            >删除</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import productHttp from '@/api/productHttp'

const tree = ref([])
const current = ref(null)
const mode = ref('') // '' | 'edit' | 'add-root' | 'add-child'
const formRef = ref(null)
const form = reactive({ id: null, name: '', sort: 0, status: 0, parentId: 0 })
const rules = { name: [{ required: true, message: '类目名称不能为空', trigger: 'blur' }] }

const formTitle = computed(() => {
  if (mode.value === 'edit') return '编辑类目：' + (current.value ? current.value.name : '')
  if (mode.value === 'add-root') return '新增一级类目'
  if (mode.value === 'add-child') return '新增子类目（上级：' + (current.value ? current.value.name : '') + '）'
  return '类目编辑'
})

const loadTree = async () => {
  const res = await productHttp.selectCategoryTree()
  tree.value = res.data
}

const onSelect = data => {
  current.value = data
  mode.value = 'edit'
  Object.assign(form, { id: data.id, name: data.name, sort: data.sort, status: data.status, parentId: data.parentId })
}

const startAddRoot = () => {
  current.value = null
  mode.value = 'add-root'
  Object.assign(form, { id: null, name: '', sort: 0, status: 0, parentId: 0 })
}

const startAddChild = () => {
  if (!current.value) return
  if (current.value.level >= 3) {
    ElMessage.warning('类目最多三级，不能在三级类目下再加')
    return
  }
  mode.value = 'add-child'
  Object.assign(form, { id: null, name: '', sort: 0, status: 0, parentId: current.value.id })
}

const submit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    if (mode.value === 'edit') {
      await productHttp.updateCategory({ id: form.id, name: form.name, sort: form.sort, status: form.status })
      ElMessage.success('修改成功')
    } else {
      await productHttp.addCategory({ name: form.name, sort: form.sort, status: form.status, parentId: form.parentId })
      ElMessage.success('新增成功')
    }
    mode.value = ''
    current.value = null
    loadTree()
  })
}

const handleDelete = () => {
  ElMessageBox.confirm(`确定删除类目「${current.value.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await productHttp.delCategory(current.value.id)
      ElMessage.success('删除成功')
      mode.value = ''
      current.value = null
      loadTree()
    })
    .catch(() => {})
}

onMounted(loadTree)
</script>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cat-form {
  max-width: 420px;
}
</style>
