<template>
  <div class="chat-widget">
    <!-- 悬浮按钮 -->
    <div class="chat-fab" @click="toggle">
      <span v-if="!visible">客服</span>
      <span v-else>收起</span>
    </div>

    <!-- 聊天面板 -->
    <div v-show="visible" class="chat-panel">
      <div class="chat-head">
        <span>智能客服</span>
        <span class="chat-sub">置信度 ≥ 85% 自动回复，否则转人工</span>
      </div>
      <div class="chat-body" ref="bodyRef">
        <div class="msg bot">
          <div class="bubble">你好，我是智能客服小 e，请描述你的问题（如：如何申请退款 / 多久发货）</div>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="bubble">
            <div>{{ m.text }}</div>
            <div v-if="m.confidence !== undefined" class="meta">
              置信度 {{ (m.confidence * 100).toFixed(0) }}%
              <el-tag v-if="m.transfer" size="small" type="warning">已转人工</el-tag>
            </div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input
          v-model="question"
          size="small"
          placeholder="输入问题，回车发送"
          @keyup.enter="send"
        />
        <el-button type="primary" size="small" :loading="asking" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import chatHttp from '@/api/chatHttp'

const visible = ref(false)
const question = ref('')
const asking = ref(false)
const messages = ref([])
const bodyRef = ref(null)

const toggle = () => {
  visible.value = !visible.value
}

const scrollBottom = async () => {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

const send = async () => {
  const q = question.value.trim()
  if (!q) {
    ElMessage.warning('请输入问题')
    return
  }
  messages.value.push({ role: 'user', text: q })
  question.value = ''
  asking.value = true
  scrollBottom()
  try {
    const res = await chatHttp.ask(q)
    const d = res.data
    messages.value.push({
      role: 'bot',
      text: d.reply,
      confidence: Number(d.confidence),
      transfer: d.transfer
    })
  } catch (e) {
    messages.value.push({
      role: 'bot',
      text: '服务暂时不可用，请稍后再试（' + ((e && e.message) || '未知错误') + '）'
    })
  } finally {
    asking.value = false
    scrollBottom()
  }
}
</script>

<style scoped>
.chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

.chat-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
  user-select: none;
  font-size: 14px;
}

.chat-panel {
  position: absolute;
  right: 0;
  bottom: 68px;
  width: 330px;
  height: 420px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-head {
  background: #409eff;
  color: #fff;
  padding: 10px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-sub {
  font-size: 11px;
  opacity: 0.85;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f5f7fa;
}

.msg {
  display: flex;
  margin-bottom: 10px;
}

.msg.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 78%;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
}

.msg.user .bubble {
  background: #d9ecff;
}

.meta {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}

.chat-input {
  display: flex;
  gap: 6px;
  padding: 10px;
  border-top: 1px solid #ebeef5;
}
</style>
