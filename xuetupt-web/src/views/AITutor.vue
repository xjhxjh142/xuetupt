<template>
  <div class="ai-tutor">
    <!-- 顶部导航 -->
    <NavBar />

    <div class="tutor-container">
      <!-- 左侧：对话区域 -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-left">
            <el-icon :size="28" color="#409eff"><MagicStick /></el-icon>
            <div>
              <h2>AI 辅导老师</h2>
              <p class="subtitle">苏格拉底式引导教学，帮你自己找到答案</p>
            </div>
          </div>
          <div class="header-right">
            <el-tag v-if="agentStatus === 'connected'" type="success" effect="dark" size="small">
              <el-icon style="margin-right: 4px"><CircleCheck /></el-icon>已连接
            </el-tag>
            <el-tag v-else type="danger" effect="dark" size="small">
              <el-icon style="margin-right: 4px"><Warning /></el-icon>未连接
            </el-tag>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-if="messages.length === 0" class="empty-state">
            <el-icon :size="64" color="#dcdfe6"><ChatLineSquare /></el-icon>
            <p>输入数学题目，开始 AI 辅导</p>
            <p class="hint">支持文字输入或拍照上传题目</p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message-item', msg.role === 'user' ? 'user-message' : 'ai-message']"
          >
            <div class="message-avatar">
              <el-avatar :size="36" v-if="msg.role === 'user'" icon="UserFilled" />
              <el-avatar :size="36" v-else style="background: #409eff" icon="MagicStick" />
            </div>
            <div class="message-content">
              <div class="message-bubble" v-html="msg.content"></div>
              <img v-if="msg.image" :src="msg.image" class="message-image" alt="函数图像" />
              <div class="message-meta">
                <span class="message-time">{{ msg.time }}</span>
                <span v-if="msg.type === 'explain'" class="message-type explain-tag">讲解</span>
                <span v-else-if="msg.type === 'praise'" class="message-type praise-tag">表扬</span>
                <span v-else-if="msg.type === 'summarize'" class="message-type summarize-tag">总结</span>
                <span v-else class="message-type hint-tag">引导</span>
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="message-item ai-message">
            <div class="message-avatar">
              <el-avatar :size="36" style="background: #409eff" icon="MagicStick" />
            </div>
            <div class="message-content">
              <div class="message-bubble loading-bubble">
                <span class="dot-pulse"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <div class="input-tools">
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              style="display:none"
              @change="handleFileSelect"
            />
            <el-tooltip content="上传题目图片" placement="top">
              <el-button circle @click="$refs.fileInput.click()">
                <el-icon><Camera /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="清空对话" placement="top">
              <el-button circle @click="clearDialog">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入数学题目，例如：解方程 2x + 3 = 7"
              @keydown.enter.prevent="sendMessage"
              :disabled="loading"
            />
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!inputText.trim()"
              @click="sendMessage"
              class="send-btn"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：知识面板 -->
      <div class="knowledge-panel" v-if="showKnowledge">
        <div class="panel-header">
          <h3><el-icon><Collection /></el-icon> 相关知识</h3>
        </div>
        <div class="panel-body">
          <div v-if="currentKnowledge.length === 0" class="panel-empty">
            <p>开始对话后，这里会显示相关的知识点</p>
          </div>
          <div v-for="(item, index) in currentKnowledge" :key="index" class="knowledge-card">
            <div class="knowledge-title">{{ item.name }}</div>
            <div class="knowledge-desc">{{ item.definition }}</div>
            <div v-if="item.formula" class="knowledge-formula">
              <span class="label">公式：</span>{{ item.formula }}
            </div>
            <div class="knowledge-grade">
              <el-tag size="small">{{ item.grade }}</el-tag>
              <el-tag size="small" :type="item.difficulty === '困难' ? 'danger' : item.difficulty === '中等' ? 'warning' : 'success'">
                {{ item.difficulty }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 图片识别确认对话框 -->
  <el-dialog v-model="showRecDialog" title="拍照识别结果" width="90%" max-width="500px">
    <div v-if="recImageUrl" style="text-align:center;margin-bottom:12px">
      <img :src="recImageUrl" style="max-width:100%;max-height:200px;border-radius:8px;border:1px solid #eee" />
    </div>
    <el-input v-model="recProblem" type="textarea" :rows="5" />
    <template #footer>
      <el-button @click="showRecDialog = false">取消</el-button>
      <el-button type="primary" @click="confirmRecognition">确认并提问</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi, aiStreamApi } from '@/api'
import NavBar from '@/components/NavBar.vue'

// ============================================================
// 配置
// ============================================================
const USE_STREAM = true  // 是否使用流式输出

// ============================================================
// 状态
// ============================================================
const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const agentStatus = ref('disconnected')
const showKnowledge = ref(true)
const currentKnowledge = ref([])
const dialogContext = ref(null)
const messageListRef = ref(null)

// 流式输出相关
const streamingMessage = ref(null)  // 当前正在流式输出的消息
const streamBuffer = ref('')        // 流式缓冲区

// 识别对话框
const showRecDialog = ref(false)
const recImageUrl = ref('')
const recProblem = ref('')
const recDialogId = ref('')

// ============================================================
// 工具函数
// ============================================================

// 格式化时间
const formatTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// ============================================================
// 服务状态检查
// ============================================================

// 检查服务状态
const checkHealth = async () => {
  try {
    const res = await aiApi.healthCheck()
    console.log('Health check response:', res)
    if (res && res.status === 'ok') {
      agentStatus.value = 'connected'
    } else {
      console.warn('Health check unexpected response:', res)
      agentStatus.value = 'disconnected'
    }
  } catch (e) {
    console.error('Health check failed:', e.message)
    agentStatus.value = 'disconnected'
  }
}

// ============================================================
// 流式输出处理
// ============================================================

/**
 * 处理流式输出的每个 token
 */
const handleStreamChunk = (text) => {
  streamBuffer.value += text
  
  if (streamingMessage.value) {
    // 更新已有的流式消息
    streamingMessage.value.content = streamBuffer.value
  } else {
    // 创建新的流式消息
    streamingMessage.value = {
      role: 'assistant',
      content: streamBuffer.value,
      type: 'hint',
      time: formatTime(),
      isStreaming: true  // 标记为流式输出中
    }
    messages.value.push(streamingMessage.value)
  }
  scrollToBottom()
}

/**
 * 流式输出完成
 */
const handleStreamDone = () => {
  if (streamingMessage.value) {
    streamingMessage.value.isStreaming = false
    streamingMessage.value = null
  }
  streamBuffer.value = ''
  loading.value = false
  scrollToBottom()
}

/**
 * 流式输出错误
 */
const handleStreamError = (error) => {
  console.error('Stream error:', error)
  if (streamingMessage.value) {
    streamingMessage.value.content += '\n\n[连接中断，请重试]'
    streamingMessage.value.isStreaming = false
    streamingMessage.value = null
  }
  streamBuffer.value = ''
  loading.value = false
  ElMessage.error('AI 回复中断，请重试')
}

// ============================================================
// 发送消息（支持流式和非流式）
// ============================================================

/**
 * 处理非流式响应
 */
const handleNonStreamResponse = (result) => {
  if (result && result.success) {
    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: result.response,
      type: result.response_type || 'hint',
      time: formatTime(),
      image: result.image_base64 || ''
    })

    // 保存上下文
    dialogContext.value = result.context

    // 更新知识面板
    if (result.context && result.context.knowledge_context) {
      updateKnowledge(result.context)
    }

    // 检查是否完成
    if (result.is_completed) {
      setTimeout(() => {
        ElMessage.success('辅导完成！可以开始新的题目了')
      }, 500)
    }
  } else {
    ElMessage.error(result?.error || 'AI 回复失败')
  }
}

/**
 * 处理流式响应
 */
const handleStreamResponse = async (text) => {
  const callbacks = {
    onChunk: handleStreamChunk,
      onDone: (dialogId, imageBase64) => {
        if (imageBase64) {
          const lastMsg = messages.value[messages.value.length - 1]
          if (lastMsg && lastMsg.role === 'assistant') {
            lastMsg.image = imageBase64
          }
        }
        handleStreamDone()
        if (dialogId) {
          dialogContext.value = { dialog_id: dialogId }
        }
      },
    onError: handleStreamError,
  }

  if (!dialogContext.value) {
    await aiStreamApi.startDialog(text, '', callbacks)
  } else {
    await aiStreamApi.continueDialog(dialogContext.value, text, callbacks)
  }
}
/**
 * 发送消息
 */
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    time: formatTime()
  })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    if (USE_STREAM) {
      // 流式模式
      await handleStreamResponse(text)
    } else {
      // 非流式模式
      let result
      if (!dialogContext.value) {
        result = await aiApi.startDialog(text)
      } else {
        result = await aiApi.continueDialog(dialogContext.value, text)
      }
      handleNonStreamResponse(result)
      loading.value = false
      scrollToBottom()
    }
  } catch (e) {
    console.error('Send message error:', e)
    ElMessage.error('网络错误，请检查 AI 服务是否启动')
    loading.value = false
  }
}

// 处理图片上传
const handleFileSelect = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  loading.value = true
  try {
    const res = await aiApi.recognizeImage(file)
    if (res && res.success) {
      // 预览图片
      const reader = new FileReader()
      reader.onload = (ev) => { recImageUrl.value = ev.target.result }
      reader.readAsDataURL(file)
      recProblem.value = res.problem
      recDialogId.value = res.dialog_id
      showRecDialog.value = true
    } else {
      ElMessage.error('图片识别失败')
    }
  } catch (err) {
    ElMessage.error('识别失败：' + (err.message || '请检查服务'))
  } finally {
    loading.value = false
    e.target.value = ''
  }
}

const confirmRecognition = () => {
  showRecDialog.value = false
  messages.value.push({
    role: 'user',
    content: `[拍照识别] ${recProblem.value}`,
    time: formatTime()
  })
  // 用已有 dialog_id 获取 AI 回复（跳过重复识别）
  if (recDialogId.value) {
    dialogContext.value = { dialog_id: recDialogId.value }
    // 用原问题的后半段模拟继续对话
    handleStreamResponse(recProblem.value)
  }
}

// 更新知识面板
const updateKnowledge = (context) => {
  if (context.knowledge_context) {
    // 简单解析知识点
    const lines = context.knowledge_context.split('\n')
    const knowledge = []
    let current = {}
    for (const line of lines) {
      if (line.startsWith('知识点：')) {
        if (current.name) knowledge.push(current)
        current = { name: line.replace('知识点：', '') }
      } else if (line.startsWith('定义：')) {
        current.definition = line.replace('定义：', '')
      } else if (line.startsWith('公式：')) {
        current.formula = line.replace('公式：', '')
      } else if (line.startsWith('年级：')) {
        current.grade = line.replace('年级：', '')
      } else if (line.startsWith('难度：')) {
        current.difficulty = line.replace('难度：', '')
      }
    }
    if (current.name) knowledge.push(current)
    currentKnowledge.value = knowledge
  }
}

// 清空对话
const clearDialog = () => {
  ElMessageBox.confirm('确定清空当前对话吗？', '提示').then(() => {
    messages.value = []
    dialogContext.value = null
    currentKnowledge.value = []
    ElMessage.success('已清空对话')
  }).catch(() => {})
}

// 定时检查服务状态
let healthTimer = null
onMounted(() => {
  checkHealth()
  healthTimer = setInterval(checkHealth, 30000)
})

onUnmounted(() => {
  if (healthTimer) clearInterval(healthTimer)
})
</script>

<style scoped>
.ai-tutor {
  min-height: 100vh;
  background: #f0f2f5;
}

.tutor-container {
  display: flex;
  height: calc(100vh - 60px);
  padding: 16px;
  gap: 16px;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 18px;
  color: #303133;
  margin: 0;
}

.subtitle {
  font-size: 12px;
  color: #909399;
  margin: 2px 0 0;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 15px;
}

.empty-state .hint {
  font-size: 13px;
  color: #c0c4cc;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.user-message .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-bubble {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.message-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  margin-top: 8px;
  border: 1px solid #e8e8e8;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding: 0 4px;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
}

.message-type {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}

.hint-tag { background: #ecf5ff; color: #409eff; }
.explain-tag { background: #fef0f0; color: #f56c6c; }
.praise-tag { background: #f0f9eb; color: #67c23a; }
.summarize-tag { background: #fdf6ec; color: #e6a23c; }

/* 加载动画 */
.loading-bubble {
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dot-pulse {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* 输入区域 */
.input-section {
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
}

.send-btn {
  height: 56px;
  width: 100px;
  font-size: 14px;
}

/* 右侧知识面板 */
.knowledge-panel {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
  font-size: 13px;
  padding: 20px;
  text-align: center;
}

.knowledge-card {
  padding: 12px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.knowledge-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
}

.knowledge-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 6px;
}

.knowledge-formula {
  font-size: 12px;
  color: #409eff;
  margin-bottom: 6px;
  font-family: 'Courier New', monospace;
}

.knowledge-formula .label {
  color: #909399;
}

.knowledge-grade {
  display: flex;
  gap: 4px;
}

@media (max-width: 768px) {
  .knowledge-panel {
    display: none;
  }
}
</style>
