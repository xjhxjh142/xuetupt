import axios from 'axios'

// 创建独立的 axios 实例（不使用全局 /api baseURL）
const aiAxios = axios.create({ baseURL: '' })
// 为 AI 实例添加 token 拦截器
aiAxios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})
// AI 实例响应拦截器 - 提取 data，统一错误处理
aiAxios.interceptors.response.use(
  response => response.data,
  error => {
    // 如果服务器返回了错误响应体，尝试提取错误信息
    if (error.response && error.response.data) {
      return Promise.reject(new Error(error.response.data.error || error.response.data.message || `请求失败 (${error.response.status})`))
    }
    return Promise.reject(error)
  }
)

// ========== 用户相关 ==========
export const userApi = {
  sendCode(phone) {
    return axios.post('/user/code', null, { params: { phone } })
  },
  login(phone, code) {
    return axios.post('/user/login', null, { params: { phone, code } })
  },
  loginForTest() {
    return axios.post('/user/login/test')
  },
  getInfo() {
    return axios.get('/user/info')
  }
}

// ========== 课程相关 ==========
export const courseApi = {
  getById(id) {
    return axios.get(`/course/${id}`)
  },
  getList(type, current = 1) {
    return axios.get('/course/list', { params: { type, current } })
  },
  getHotList() {
    return axios.get('/course/hot')
  },
  seckill(courseId) {

    return axios.post(`/course/seckill/${courseId}`)
  },
  pay(orderNo) {
    return axios.post('/course/pay', { orderNo })
  }
}

// ========== 资料相关 ==========
export const materialApi = {
  getById(id) {
    return axios.get(`/material/${id}`)
  },
  getList(type, current = 1) {
    return axios.get('/material/list', { params: { type, current } })
  },
  seckill(materialId) {
    return axios.post(`/material/seckill/${materialId}`)
  },
  getDownloadUrl(orderNo) {
    return axios.get('/material/download', { params: { orderNo } })
  }
}

// ========== 教师相关 ==========
export const teacherApi = {
  getById(id) {
    return axios.get(`/teacher/${id}`)
  },
  getList(current = 1) {
    return axios.get('/teacher/list', { params: { current } })
  }
}

// ========== 自习室相关 ==========
export const studyRoomApi = {
  getList() {
    return axios.get('/room/list')
  },
  enter(roomId) {
    return axios.post(`/room/enter/${roomId}`)
  },
  leave(roomId) {
    return axios.post(`/room/leave/${roomId}`)
  },
  startFocus(roomId) {
    return axios.post(`/room/focus/start/${roomId}`)
  },
  endFocus(roomId) {
    return axios.post(`/room/focus/end/${roomId}`)
  },
  getDailyRank() {
    return axios.get('/room/rank/daily')
  },
  getWeeklyRank() {
    return axios.get('/room/rank/weekly')
  },
  getTotalRank() {
    return axios.get('/room/rank/total')
  },
  sign() {
    return axios.post('/room/sign')
  },
  getSignStatus() {
    return axios.get('/room/sign/status')
  }
}

// ========== AI 辅导相关 ==========
// 使用独立的 axios 实例（避免被全局 /api baseURL 影响）
// 通过 vite 代理 /ai -> localhost:8013
const AI_BASE = '/ai'
export const aiApi = {
  // 开始新的辅导对话
  startDialog(problem, userId = '', dialogId = '') {
    return aiAxios.post(`${AI_BASE}/api/agent/start`, { problem, user_id: userId, dialog_id: dialogId })
  },
  // 继续辅导对话
  continueDialog(context, answer) {
    return aiAxios.post(`${AI_BASE}/api/agent/continue`, { context, answer })
  },
  // 开始新的辅导对话（流式版）
  startDialogStream(problem, userId = '', dialogId = '') {
    return aiAxios.post(`${AI_BASE}/api/agent/start/stream`, { problem, user_id: userId, dialog_id: dialogId }, {
      responseType: 'stream',
      adapter: 'fetch'  // 使用 fetch adapter 以支持流式读取
    })
  },
  // 继续辅导对话（流式版）
  continueDialogStream(context, answer) {
    return aiAxios.post(`${AI_BASE}/api/agent/continue/stream`, { context, answer }, {
      responseType: 'stream',
      adapter: 'fetch'
    })
  },
  // 识别图片中的数学题目
  recognizeImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return aiAxios.post(`${AI_BASE}/api/agent/recognize`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  // 健康检查
  healthCheck() {
    return aiAxios.get(`${AI_BASE}/health`)
  }
}

/**
 * 使用原生 fetch 进行 SSE 流式请求
 * 因为 axios 对流式响应的支持不够完善
 */
export const aiStreamApi = {
  /**
   * 发送流式请求，逐块处理 SSE 数据
   * @param {string} url - API 路径（相对于 /ai）
   * @param {object} body - 请求体
   * @param {function} onChunk - 每个 token 的回调 (text: string) => void
   * @param {function} onDone - 完成回调 () => void
   * @param {function} onError - 错误回调 (err: Error) => void
   */
  async streamRequest(url, body, { onChunk, onDone, onError }) {
    try {
      const token = localStorage.getItem('token')
      const headers = { 'Content-Type': 'application/json' }
      if (token) headers['Authorization'] = token

      const response = await fetch(`${AI_BASE}${url}`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      })

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}))
        throw new Error(errData.detail || `请求失败 (${response.status})`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let dialogId = ''
      let imageBase64 = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 数据
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''  // 保留未完成的行

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data === '[DONE]') {
              onDone && onDone(dialogId, imageBase64)
              return
            }
            try {
              const parsed = JSON.parse(data)
              const content = parsed.choices?.[0]?.delta?.content || ''
              if (content) {
                onChunk && onChunk(content)
              }
              if (parsed.dialog_id) {
                dialogId = parsed.dialog_id
              }
              if (parsed.image_base64) {
                imageBase64 = parsed.image_base64
              }
              if (parsed.finish_reason === 'stop') {
                onDone && onDone(dialogId, imageBase64)
                return
              }
            } catch (e) {
              // 忽略解析错误
            }
          }
        }
      }
      onDone && onDone(dialogId, imageBase64)
    } catch (e) {
      onError && onError(e)
    }
  },

  // 开始新的辅导对话（流式）
  startDialog(problem, dialogId = '', callbacks) {
    return this.streamRequest('/api/agent/start/stream', {
      problem,
      dialog_id: dialogId,
    }, callbacks)
  },

  // 继续辅导对话（流式）
  continueDialog(context, answer, callbacks) {
    return this.streamRequest('/api/agent/continue/stream', {
      context,
      answer,
    }, callbacks)
  }
}
