<template>
  <div class="login-page">
    <div class="login-box">
      <h2 class="login-title">抢票系统</h2>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="zhangsan / admin"/>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="123456" show-password/>
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="submitLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="regForm.username"/>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="regForm.password" type="password" show-password/>
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="regForm.realName"/>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="regForm.phone" maxlength="11"/>
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="regForm.idCard" maxlength="18"/>
            </el-form-item>
            <el-button type="success" class="submit-btn" :loading="loading" @click="submitRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="tip muted">体验账号：zhangsan / admin123，管理员：admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const regFormRef = ref()

const loginForm = ref({ username: 'zhangsan', password: '123456' })
const regForm = ref({ username: '', password: '', realName: '', phone: '', idCard: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}
const regRules = {
  username: [{ required: true, min: 2, max: 32, message: '用户名 2~32 位', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 32, message: '密码 6~32 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  idCard: [{ pattern: /^(\d{17}[0-9Xx]|\d{15})$/, message: '身份证号格式不正确', trigger: 'blur' }]
}

async function submitLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    await userStore.doLogin(loginForm.value)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/trains')
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  await regFormRef.value.validate()
  loading.value = true
  try {
    await userStore.doRegister(regForm.value)
    ElMessage.success('注册成功')
    router.push('/trains')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  width: 420px;
  background: #fff;
  border-radius: 10px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, .08);
}

.login-title {
  text-align: center;
  color: #1a73e8;
  margin-bottom: 18px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.tip {
  margin-top: 14px;
  text-align: center;
}
</style>
