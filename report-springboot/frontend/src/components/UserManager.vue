<template>
  <div>
    <el-card shadow="never" style="margin-bottom:16px; border:1px solid #e4e7ed;">
      <template #header>
        <div style="display:flex; align-items:center;">
          <el-icon style="margin-right:8px; color:#67c23a; font-size:18px;"><Plus /></el-icon>
          <span style="font-weight:600; font-size:15px;">新增管理员</span>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="default"
        style="padding:4px 0;"
        @submit.prevent="addUser">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录账号" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="至少 6 位" show-password maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" style="width:100%;">
                <el-option label="管理员（完全权限）" value="ADMIN" />
                <el-option label="运维（受限权限）" value="OPS" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24" style="display:flex; justify-content:flex-end; gap:12px;">
            <el-button type="primary" native-type="submit" :loading="submitting" size="default">
              <el-icon style="margin-right:4px;"><Select /></el-icon>确认新增
            </el-button>
            <el-button size="default" @click="resetForm">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <span style="font-weight:600;">
            <el-icon style="vertical-align:middle; margin-right:4px;"><User /></el-icon>
            管理员列表
          </span>
          <el-button size="small" @click="load">
            <el-icon style="margin-right:4px;"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-table :data="users" border stripe style="width:100%;" size="small" v-loading="loading">
        <el-table-column label="ID" width="60" prop="id" />
        <el-table-column label="用户名" width="120" prop="username" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'warning'" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
              {{ row.enabled ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">{{ formatTime(row.lastLogin) || '从未登录' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :disabled="row.username === currentUser"
              @change="toggleUser(row)"
              size="small"
              style="--el-switch-on-color:#13ce66; --el-switch-off-color:#ff4949; margin-right:8px;" />
            <el-button size="small" type="primary" link @click="showResetPwd(row)">改密</el-button>
            <el-button
              size="small"
              type="danger"
              link
              :disabled="row.username === currentUser"
              @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="360px">
      <el-form @submit.prevent="resetPassword">
        <el-form-item label="新密码">
          <el-input v-model="pwdDialog.password" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-button type="primary" @click="resetPassword" style="width:100%;">确认重置</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import { Plus, Refresh, User, Select } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api.js'

export default {
  components: { Plus, Refresh, User, Select },
  props: { currentUser: { type: String, default: 'admin' } },
  data() {
    return {
      users: [],
      loading: false,
      submitting: false,
      formRef: null,
      form: { username: '', password: '', role: 'ADMIN' },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 30, message: '用户名长度 2~30 位', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '仅支持字母、数字、下划线和中文', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 50, message: '密码长度 6~50 位', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      pwdDialog: { visible: false, userId: null, username: '', password: '' }
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        const d = await apiGet('/api/admin/users', { noRedirect: true })
        if (d.success) this.users = d.data || []
      } catch {}
      this.loading = false
    },
    async addUser() {
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) {
        ElMessage.warning('请检查表单中的错误提示')
        return
      }
      this.submitting = true
      try {
        const d = await apiPost('/api/admin/users', this.form, { noRedirect: true })
        if (d.success) {
          ElMessage.success('管理员已创建')
          this.resetForm()
          this.load()
        } else {
          ElMessage.error(d.message)
        }
      } catch { ElMessage.error('请求失败') }
      this.submitting = false
    },
    resetForm() {
      this.form = { username: '', password: '', role: 'ADMIN' }
      this.$refs.formRef?.clearValidate()
    },
    async toggleUser(row) {
      try {
        const d = await apiPut(`/api/admin/users/${row.id}`, { enabled: row.enabled }, { noRedirect: true })
        if (!d.success) { this.load(); ElMessage.error(d.message) }
        else ElMessage.success(row.enabled ? '已启用' : '已封禁')
      } catch { this.load(); ElMessage.error('请求失败') }
    },
    showResetPwd(row) {
      this.pwdDialog = { visible: true, userId: row.id, username: row.username, password: '' }
    },
    async resetPassword() {
      if (!this.pwdDialog.password || this.pwdDialog.password.length < 6) {
        ElMessage.warning('密码至少 6 位'); return
      }
      try {
        const d = await apiPut(`/api/admin/users/${this.pwdDialog.userId}/password`,
          { password: this.pwdDialog.password }, { noRedirect: true })
        if (d.success) {
          ElMessage.success('密码已重置')
          this.pwdDialog.visible = false
        } else { ElMessage.error(d.message) }
      } catch { ElMessage.error('请求失败') }
    },
    async deleteUser(row) {
      try {
        await ElMessageBox.confirm(`确定删除管理员「${row.username}」？此操作不可撤销。`, '警告',
          { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
        const d = await apiDelete(`/api/admin/users/${row.id}`, { noRedirect: true })
        if (d.success) { ElMessage.success('已删除'); this.load() }
        else ElMessage.error(d.message)
      } catch {}
    },
    formatTime(t) {
      if (!t) return '-'
      return t.substring(0, 19).replace('T', ' ')
    }
  }
}
</script>
