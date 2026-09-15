<template>
  <div>
    <el-tabs v-model="activeTab" class="kb-tabs">
      <!-- 知识库 -->
      <el-tab-pane label="知识库" name="knowledge">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input v-model="query.question" placeholder="按标准问法搜索" clearable style="width: 220px" @keyup.enter="loadKnowledge" />
            <el-button type="primary" @click="loadKnowledge">查询</el-button>
            <el-button type="success" v-hasPermi="'knowledge:add'" @click="openAdd">新增知识</el-button>
          </div>
          <el-table :data="knowledgeList" border stripe v-loading="loading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="question" label="标准问法" min-width="150" />
            <el-table-column label="相似问法" min-width="200">
              <template #default="{ row }">
                <el-tag v-for="(q, i) in parseSimilar(row.similarQuestions)" :key="i" size="small" class="sim-tag">{{ q }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="answer" label="标准答案" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '生效' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" v-hasPermi="'knowledge:edit'" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" v-hasPermi="'knowledge:del'" @click="doDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            background
            layout="total, prev, pager, next"
            :total="total"
            v-model:current-page="query.pageNum"
            :page-size="query.pageSize"
            @current-change="loadKnowledge"
          />
        </el-card>
      </el-tab-pane>

      <!-- 待补充问题 -->
      <el-tab-pane label="待补充问题" name="pending">
        <el-card shadow="never">
          <div class="tip-line">转人工且当前知识库仍无法回答的问题；补充知识后会自动移出此列表</div>
          <el-table :data="pendingList" border stripe v-loading="loading">
            <el-table-column prop="question" label="问题" min-width="240" />
            <el-table-column prop="count" label="出现次数" width="100" />
            <el-table-column label="最近时间" min-width="170">
              <template #default="{ row }">{{ fmt(row.lastTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button link type="success" v-hasPermi="'knowledge:add'" @click="addFromPending(row)">补充知识</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !pendingList.length" description="暂无待补充问题" :image-size="70" />
        </el-card>
      </el-tab-pane>

      <!-- 会话记录 -->
      <el-tab-pane label="会话记录" name="logs">
        <el-card shadow="never">
          <el-table :data="logs" border stripe v-loading="loading">
            <el-table-column prop="question" label="用户问题" min-width="180" />
            <el-table-column prop="answer" label="机器人回答" min-width="240" show-overflow-tooltip />
            <el-table-column label="来源" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.source === 'rule'" type="success" size="small">知识库</el-tag>
                <el-tag v-else-if="row.source === 'llm'" type="primary" size="small">AI 回答</el-tag>
                <el-tag v-else-if="row.source === 'transfer'" type="warning" size="small">转人工</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="90">
              <template #default="{ row }">{{ row.confidence == null ? '-' : row.confidence }}</template>
            </el-table-column>
            <el-table-column label="是否转人工" width="110">
              <template #default="{ row }">
                <el-tag :type="row.isTransfer === 1 ? 'warning' : 'success'">{{ row.isTransfer === 1 ? '转人工' : '自动回复' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="170">
              <template #default="{ row }">{{ fmt(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            background
            layout="total, prev, pager, next"
            :total="logTotal"
            v-model:current-page="logQuery.pageNum"
            :page-size="logQuery.pageSize"
            @current-change="loadLogs"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑知识 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识' : '新增知识'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标准问法" prop="question">
          <el-input v-model="form.question" />
        </el-form-item>
        <el-form-item label="相似问法">
          <el-input v-model="similarText" placeholder="多个问法用中文逗号或英文逗号分隔" />
        </el-form-item>
        <el-form-item label="标准答案" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="生效" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import knowledgeHttp from '@/api/knowledgeHttp'
import chatHttp from '@/api/chatHttp'

const activeTab = ref('knowledge')
const loading = ref(false)

// ---------- 知识库 ----------
const knowledgeList = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, question: '' })

const loadKnowledge = async () => {
  loading.value = true
  try {
    const res = await knowledgeHttp.selectKnowledgeByPage(query)
    knowledgeList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const parseSimilar = text => {
  try {
    return JSON.parse(text || '[]')
  } catch (e) {
    return []
  }
}

// ---------- 待补充 ----------
const pendingList = ref([])
const loadPending = async () => {
  loading.value = true
  try {
    const res = await chatHttp.selectPending()
    pendingList.value = res.data
  } finally {
    loading.value = false
  }
}

// ---------- 会话记录 ----------
const logs = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10 })
const loadLogs = async () => {
  loading.value = true
  try {
    const res = await chatHttp.selectLogsByPage(logQuery)
    logs.value = res.data.records
    logTotal.value = res.data.total
  } finally {
    loading.value = false
  }
}

watch(activeTab, tab => {
  if (tab === 'knowledge') loadKnowledge()
  else if (tab === 'pending') loadPending()
  else loadLogs()
})

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, question: '', answer: '', category: '', status: 1 })
const similarText = ref('')
const rules = {
  question: [{ required: true, message: '标准问法不能为空', trigger: 'blur' }],
  answer: [{ required: true, message: '标准答案不能为空', trigger: 'blur' }]
}

const openAdd = () => {
  Object.assign(form, { id: null, question: '', answer: '', category: '', status: 1 })
  similarText.value = ''
  dialogVisible.value = true
}

const openEdit = row => {
  Object.assign(form, row)
  similarText.value = parseSimilar(row.similarQuestions).join(',')
  dialogVisible.value = true
}

const addFromPending = row => {
  Object.assign(form, { id: null, question: row.question, answer: '', category: '', status: 1 })
  similarText.value = ''
  dialogVisible.value = true
}

const submit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    const similar = similarText.value
      .split(/[,，]/)
      .map(s => s.trim())
      .filter(Boolean)
    const payload = { ...form, similarQuestions: JSON.stringify(similar) }
    if (form.id) {
      await knowledgeHttp.updateKnowledge(payload)
      ElMessage.success('修改成功')
    } else {
      await knowledgeHttp.addKnowledge(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadKnowledge()
  })
}

const doDelete = row => {
  ElMessageBox.confirm(`确定删除知识「${row.question}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await knowledgeHttp.delKnowledge([row.id])
      ElMessage.success('删除成功')
      loadKnowledge()
    })
    .catch(() => {})
}

const fmt = t => (t ? String(t).replace('T', ' ').substring(0, 19) : '-')

onMounted(loadKnowledge)
</script>

<style scoped>
.kb-tabs {
  background: #fff;
  padding: 4px 14px 14px;
  border-radius: 4px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.sim-tag {
  margin: 2px 4px 2px 0;
}

.tip-line {
  color: #909399;
  font-size: 12px;
  margin-bottom: 10px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
