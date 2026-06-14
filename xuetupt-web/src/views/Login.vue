<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>学途</h1>
        <p>在线学习平台</p>
      </div>
      <el-form :model="form" class="login-form">
        <el-form-item>
          <el-input v-model="form.phone" placeholder="手机号" size="large" prefix-icon="Iphone" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.code" placeholder="验证码" size="large" prefix-icon="Key">
            <template #append>
              <el-button :disabled="countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button size="large" class="login-btn" @click="handleTestLogin">
            测试登录（免验证码）
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'

const router = useRouter()
const form = reactive({ phone: '13800138000', code: '' })
const countdown = ref(0)

const sendCode = async () => {
  if (!form.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  try {
    const res = await userApi.sendCode(form.phone)
    if (res.success) {
      ElMessage.success(res.data || '验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } else {
      ElMessage.error(res.errorMsg || '获取验证码失败')
    }
  } catch (err) {
    ElMessage.error('请求失败，请检查后端是否已启动')
  }
}

const handleLogin = async () => {
  if (!form.phone || !form.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const res = await userApi.login(form.phone, form.code)
  if (res.success) {
    localStorage.setItem('token', res.data)
    ElMessage.success('登录成功')
    router.push('/home')
  }
}

const handleTestLogin = async () => {
  const res = await userApi.loginForTest()
  if (res.success) {
    localStorage.setItem('token', res.data)
    ElMessage.success('测试登录成功')
    router.push('/home')
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 36px;
  color: #409eff;
  margin-bottom: 8px;
}

.login-header p {
  color: #999;
  font-size: 14px;
}

.login-form {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
}
</style>
