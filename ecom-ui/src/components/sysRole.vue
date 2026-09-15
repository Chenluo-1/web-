<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" v-hasPermi="'sys:role:add'" @click="openAdd">新增角色</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="roleKey" label="权限字符" min-width="140" />
        <el-table-column prop="roleSort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="'sys:role:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" v-hasPermi="'sys:role:setMenu'" @click="openAssign(row)">分配权限</el-button>
            <el-button link type="danger" v-hasPermi="'sys:role:del'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        @current-change="loadPage"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="460px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" :disabled="!!form.id" placeholder="如 product_op" />
        </el-form-item>
        <el-form-item label="排序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="0" :inactive-value="1" active-text="正常" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="assignVisible" :title="`分配权限 - ${assignRoleName}`" width="480px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import sysRoleHttp from '@/api/sysRoleHttp'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await sysRoleHttp.selectRoleByPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, roleName: '', roleKey: '', roleSort: 0, status: 0 })
const formRules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleKey: [{ required: true, message: '权限字符不能为空', trigger: 'blur' }]
}

const openAdd = () => {
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: 0 })
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, roleSort: row.roleSort, status: row.status })
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    if (form.id) {
      await sysRoleHttp.updateRole(form)
      ElMessage.success('修改成功')
    } else {
      await sysRoleHttp.addRole(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadPage()
  })
}

// ---------- 删除 ----------
const handleDelete = row => {
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await sysRoleHttp.delRole([row.id])
      ElMessage.success('删除成功')
      loadPage()
    })
    .catch(() => {})
}

// ---------- 分配权限 ----------
const assignVisible = ref(false)
const assignRoleName = ref('')
const menuTreeRef = ref(null)
const menuTree = ref([])
let assignRoleId = null

const openAssign = async row => {
  assignRoleId = row.id
  assignRoleName.value = row.roleName
  const treeRes = await sysRoleHttp.getMenuTree()
  menuTree.value = treeRes.data
  const checkedRes = await sysRoleHttp.getRoleMenuIds(row.id)
  assignVisible.value = true
  // 等 el-tree 渲染完再回显勾选
  setTimeout(() => {
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(checkedRes.data || [], false)
    }
  }, 0)
}

const submitAssign = async () => {
  const checked = menuTreeRef.value.getCheckedKeys()
  await sysRoleHttp.setRoleMenu(assignRoleId, checked)
  ElMessage.success('权限分配成功（重新登录后生效）')
  assignVisible.value = false
}

onMounted(loadPage)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
