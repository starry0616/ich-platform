<template>
  <div class="page-container">
    <div class="page-head">
      <h2>非遗智能问答</h2>
      <p>基于平台知识库的非遗文化智能问答助手</p>
    </div>

    <el-card shadow="never" class="chat-card">
      <div class="chat-window" ref="chatWindow">
        <div v-if="!messages.length" class="welcome">
          <el-icon class="welcome-icon"><ChatDotRound /></el-icon>
          <p>您好！我是非遗文化智能问答助手，可以回答有关非遗项目、传承人、历史渊源等方面的问题。</p>
          <p class="tips">试试问：云南有哪些国家级非遗项目？</p>
          <div class="model-note">
            <el-icon><Cpu /></el-icon>
            <span>当前回答由平台知识库检索生成；已预留免费大模型接入（智谱 GLM-4-Flash 永久免费），配置见后端 application.yml 的 ich.ai 段</span>
          </div>
        </div>
        <div v-for="(m, idx) in messages" :key="idx" class="msg-row" :class="m.role">
          <div class="bubble" :class="m.role">
            <template v-if="m.role === 'assistant'">
              <div class="ai-bubble">{{ m.content }}</div>
              <div v-if="m.refs && m.refs.length" class="refs">
                <div class="refs-title">参考来源：</div>
                <a v-for="(r, i) in m.refs" :key="i" class="ref-item" :href="r.url || 'javascript:void(0)'" target="_blank">
                  {{ r.title || r.name || `来源${i + 1}` }}
                </a>
              </div>
              <div v-if="m.chatId" class="feedback">
                <span>回答有帮助吗？</span>
                <el-button size="small" text @click="sendFeedback(m.chatId, 1)">有帮助</el-button>
                <el-button size="small" text @click="sendFeedback(m.chatId, 2)">没帮助</el-button>
              </div>
            </template>
            <template v-else>
              {{ m.content }}
            </template>
          </div>
        </div>
        <div v-if="loading" class="msg-row assistant">
          <div class="bubble assistant typing"><span></span><span></span><span></span></div>
        </div>
      </div>

      <div class="input-bar">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          maxlength="500"
          placeholder="请输入你的问题，例如：非物质文化遗产是什么？"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { chat, getChatHistory, postFeedback } from '@/api/ai'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const question = ref('')
const loading = ref(false)
const messages = ref([])
const chatWindow = ref(null)
const sessionId = ref(null)

function scrollBottom() {
  nextTick(() => {
    if (chatWindow.value) {
      chatWindow.value.scrollTop = chatWindow.value.scrollHeight
    }
  })
}

async function send() {
  const q = question.value.trim()
  if (!q) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用智能问答')
    return
  }
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  scrollBottom()
  try {
    const res = await chat({ question: q, sessionId: sessionId.value })
    const d = res.data || {}
    sessionId.value = d.sessionId || sessionId.value
    messages.value.push({
      role: 'assistant',
      content: d.answer || '抱歉，暂时无法回答该问题。',
      refs: d.refs || [],
      chatId: d.chatId
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '问答服务暂不可用：' + (e.message || '未知错误') })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

async function sendFeedback(chatId, score) {
  try {
    await postFeedback(chatId, { useful: score >= 4 })
    ElMessage.success('感谢反馈')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getChatHistory({ page: 1, size: 10 })
    const records = (res.data && (res.data.records || res.data)) || []
    if (records.length) {
      sessionId.value = records[0].sessionId || null
      // 展示最近一轮对话
      messages.value = records
        .slice(0, 3)
        .reverse()
        .map((r) => ({ role: 'assistant', content: r.answer, chatId: r.id }))
      messages.value.unshift({ role: 'user', content: records[0].question })
    }
  } catch (e) {}
})
</script>

<style scoped>
.page-head {
  margin-bottom: 18px;
}
.page-head h2 {
  font-size: 26px;
  color: #5a4632;
}
.page-head p {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
.chat-card {
  max-width: 860px;
  margin: 0 auto;
}
.chat-window {
  height: 480px;
  overflow-y: auto;
  padding: 16px;
  background:
    radial-gradient(circle at 15% 10%, rgba(139, 69, 19, 0.05), transparent 40%),
    radial-gradient(circle at 85% 90%, rgba(212, 160, 95, 0.07), transparent 45%),
    #faf7f1;
  border-radius: 10px;
}
.welcome {
  text-align: center;
  color: #999;
  padding-top: 60px;
}
.welcome-icon {
  font-size: 48px;
  color: #c9b79c;
}
.tips {
  font-size: 13px;
  color: #c9b79c;
}
.model-note {
  margin: 18px auto 0;
  max-width: 520px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fdf6ec;
  border: 1px dashed #e2cfa8;
  color: #a0703c;
  font-size: 12px;
  padding: 8px 14px;
  border-radius: 10px;
  line-height: 1.6;
}
.msg-row {
  display: flex;
  margin-bottom: 14px;
}
.msg-row.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 10px;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
}
.bubble.user {
  background: linear-gradient(135deg, #8b4513, #a0522d);
  color: #fff;
  border-bottom-right-radius: 2px;
  box-shadow: 0 3px 10px rgba(139, 69, 19, 0.25);
}
.bubble.assistant {
  background: #fff;
  color: #333;
  border: 1px solid #f0e8dc;
  border-bottom-left-radius: 2px;
  box-shadow: 0 2px 6px rgba(139, 69, 19, 0.07);
}
.ai-bubble {
  white-space: pre-wrap;
}
.refs {
  margin-top: 8px;
  border-top: 1px dashed #e0d8c8;
  padding-top: 6px;
  font-size: 12px;
}
.refs-title {
  color: #999;
  margin-bottom: 4px;
}
.ref-item {
  display: block;
  color: #8b4513;
  margin-bottom: 3px;
}
.feedback {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}
.typing {
  display: flex;
  gap: 4px;
}
.typing span {
  width: 6px;
  height: 6px;
  background: #bbb;
  border-radius: 50%;
  animation: blink 1.2s infinite;
}
.typing span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
.input-bar {
  display: flex;
  gap: 10px;
  padding-top: 12px;
  align-items: flex-end;
}
.input-bar .el-button {
  height: 56px;
}
</style>
