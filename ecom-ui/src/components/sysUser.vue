<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="登录名">
          <el-input v-model="query.username" placeholder="登录名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="昵称/姓名">
          <el-input v-model="query.nickName" placeholder="昵称/姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.userType" placeholder="全部" clearable style="width: 120px">
            <el-option label="运营" :value="0" />
            <el-option label="消费者" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" v-hasPermi="'user:list'" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" v-hasPermi="'user:add'" @click="openAdd">新增用户</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="登录名" min-width="110" />
        <el-table-column prop="nickName" label="昵称/姓名" min-width="110" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 0 ? 'primary' : 'success'">
              {{ row.userType === 0 ? '运营' : '消费者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="'user:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="info" v-hasPermi="'user:status'" @click="handleStatus(row)">
              {{ row.status === 0 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="warning" v-hasPermi="'user:resetPwd'" @click="openResetPwd(row)">重置密码</el-button>
            <el-button link type="danger" v-hasPermi="'user:delete'" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="昵称/姓名" prop="nickName">
          <el-input v-model="form.nickName" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="不填默认 123456" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-radio-group v-model="form.userType">
            <el-radio :value="0">运营</el-radio>
            <el-radio :value="1">消费者</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
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

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="用户">
          <el-input :model-value="pwdForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import sysUserHttp from '@/api/sysUserHttp'
import sysRoleHttp from '@/api/sysRoleHttp'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const roleOptions = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, username: '', nickName: '', userType: null })

const loadPage = async () => {
  loading.value = true
  try {
    const res = await sysUserHttp.selectUserByPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  loadPage()
}

const resetQuery = () => {
  query.username = ''
  query.nickName = ''
  query.userType = null
  handleSearch()
}

const handleSizeChange = () => {
  query.pageNum = 1
  loadPage()
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, username: '', nickName: '', password: '', userType: 0,
  phone: '', email: '', status: 0, roleIds: []
})
const formRules = {
  username: [{ required: true, message: '登录名不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '昵称/姓名不能为空', trigger: 'blur' }]
}

const loadRoleOptions = async () => {
  const res = await sysRoleHttp.selectAllRole()
  roleOptions.value = res.data
}

const openAdd = async () => {
  Object.assign(form, {
    id: null, username: '', nickName: '', password: '', userType: 0,
    phone: '', email: '', status: 0, roleIds: []
  })
  await loadRoleOptions()
  dialogVisible.value = true
}

const openEdit = async row => {
  const res = await sysUserHttp.getUserById(row.id)
  const roleRes = await sysUserHttp.getRoleIds(row.id)
  Object.assign(form, res.data, { password: '', roleIds: roleRes.data })
  await loadRoleOptions()
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    if (form.id) {
      await sysUserHttp.updateUser(form)
      ElMessage.success('修改成功')
    } else {
      await sysUserHttp.addUser(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadPage()
  })
}

// ---------- 删除 ----------
const handleDelete = row => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await sysUserHttp.delUser([row.id])
      ElMessage.success('删除成功')
      loadPage()
    })
    .catch(() => {})
}

// ---------- 启用/禁用 ----------
const handleStatus = row => {
  const target = row.status === 0 ? 1 : 0
  const action = target === 1 ? '停用' : '启用'
  ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await sysUserHttp.updateStatus(row.id, target)
      ElMessage.success(`${action}成功`)
      loadPage()
    })
    .catch(() => {})
}

// ---------- 重置密码 ----------
const pwdVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ id: null, username: '', newPassword: '' })
const pwdRules = {
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ]
}

const openResetPwd = row => {
  pwdForm.id = row.id
  pwdForm.username = row.username
  pwdForm.newPassword = ''
  pwdVisible.value = true
}

const submitResetPwd = () => {
  pwdFormRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    await sysUserHttp.resetPassword({ id: pwdForm.id, newPassword: pwdForm.newPassword })
    ElMessage.success('密码重置成功')
    pwdVisible.value = false
  })
}

onMounted(loadPage)
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
</style>
