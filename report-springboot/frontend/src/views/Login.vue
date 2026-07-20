<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <template #header>
        <h2 style="text-align:center; margin:0;">运营报告管理</h2>
      </template>
      <p style="text-align:center; color:#909399; margin-bottom:24px; font-size:14px;">请登录以访问后台管理</p>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom:16px;" />
      <el-form @submit.prevent="login" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="username" placeholder="请输入管理员账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" style="width:100%;" :loading="loading">登 录</el-button>
      </el-form>
    </el-card>
    <div style="position:fixed; bottom:0; left:0; right:0; text-align:center; padding:12px; color:rgba(255,255,255,0.4); font-size:12px;">
      <a href="https://beian.miit.gov.cn/" target="_blank" style="color:rgba(255,255,255,0.4); text-decoration:none;">黔ICP备19003726号-1</a>
      &nbsp;&nbsp;|&nbsp;&nbsp;
      <a href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=52011502002024" target="_blank" style="color:rgba(255,255,255,0.4); text-decoration:none;">
        <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAACXBIWXMAAAsTAAALEwEAmpwYAAABWWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyI+CiAgICAgICAgIDx0aWZmOkNvbXByZXNzaW9uPjE8L3RpZmY6Q29tcHJlc3Npb24+CiAgICAgICAgIDx0aWZmOlBPcmlkZW50YXRpb24+MTwvdGlmZjpQb3JpZW50YXRpb24+CiAgICAgIDwvcmRmOkRlc2NyaXB0aW9uPgogICA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgwldOnJAAAAXElEQVQ4EWNg+M9AAWBigALKABPAJ4EVYGJiYqBME4LhBQsWMEA1MTAwMFBkCLIAJiYmBopMGUAGIFmBpQHZCv5TbACyCxgYGCj2ApYwQBkITKA2YCsFKBUBsgBz8RqFgAes+QAAAABJRU5ErkJggg==" style="vertical-align:middle; margin-right:4px;" />
        贵公网安备 52011502002024号
      </a>
      &nbsp;&nbsp;|&nbsp;&nbsp;<span>贵州建工监理咨询有限公司</span>
    </div>
  </div>
</template>

<script>
import { User, Lock } from '@element-plus/icons-vue'

export default {
  components: { User, Lock },
  data() {
    return { username: '', password: '', error: '', loading: false }
  },
  methods: {
    async login() {
      this.loading = true
      this.error = ''
      try {
        const form = new URLSearchParams()
        form.append('username', this.username)
        form.append('password', this.password)
        const base = import.meta.env.BASE_URL

        const res = await fetch(`${base}admin/login`, {
          method: 'POST',
          credentials: 'include',
          body: form,
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        })

        const data = await res.json()
        if (data.success) {
          this.$router.push('/')
        } else {
          this.error = data.message || '登录失败'
        }
      } catch (e) {
        this.error = '网络错误，请检查后端是否启动'
      }
      this.loading = false
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card { width: 400px; }
</style>
