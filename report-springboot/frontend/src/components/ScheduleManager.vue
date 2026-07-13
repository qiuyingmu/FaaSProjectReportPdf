<template>
  <div>
    <el-card shadow="never" style="margin-bottom:16px; border:1px solid #e4e7ed;">
      <template #header>
        <div style="display:flex; align-items:center;">
          <el-icon style="margin-right:8px; color:#409eff; font-size:18px;"><Plus /></el-icon>
          <span style="font-weight:600; font-size:15px;">新增定时任务</span>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="default"
        style="padding:4px 0;"
        @submit.prevent="addTask">
        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item label="任务标识" prop="type">
              <el-input v-model="form.type" placeholder="如: daily" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="显示名称" prop="displayName">
              <el-input v-model="form.displayName" placeholder="如: 日报" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="时间范围" prop="timeRangeCode">
              <el-select v-model="form.timeRangeCode" style="width:100%;">
                <el-option label="当日" value="today" />
                <el-option label="昨日" value="yesterday" />
                <el-option label="上周" value="lastWeek" />
                <el-option label="上月" value="lastMonth" />
                <el-option label="上季度" value="lastQuarter" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
                <el-option label="本季度" value="quarter" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Cron 表达式" prop="cron">
              <el-input v-model="form.cron" placeholder="0 0 10 * * MON" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="任务说明">
              <el-input v-model="form.description" placeholder="描述该任务的用途" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12" style="display:flex; align-items:flex-end; padding-bottom:20px;">
            <el-button type="primary" native-type="submit" :loading="submitting" size="default">
              <el-icon style="margin-right:4px;"><Select /></el-icon>确认新增
            </el-button>
            <el-button size="default" @click="resetForm" style="margin-left:12px;">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <span style="font-weight:600;">
            <el-icon style="vertical-align:middle; margin-right:4px;"><Clock /></el-icon>
            已配置任务
          </span>
          <el-button size="small" @click="load">
            <el-icon style="margin-right:4px;"><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-table :data="tasks" border stripe style="width:100%;" size="small">
        <el-table-column label="名称" width="80">
          <template #default="{ row }"><strong>{{ row.displayName }}</strong></template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column label="时间范围" width="100">
          <template #default="{ row }">{{ row.timeRangeCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="Cron" width="220">
          <template #default="{ row }">
            <el-input v-model="row.cron" size="small" style="width:140px;" />
            <el-button size="small" type="primary" link @click="updateCron(row)">保存</el-button>
          </template>
        </el-table-column>
        <el-table-column label="说明">
          <template #default="{ row }">
            <span style="color:#909399; font-size:12px;">{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
              {{ row.enabled ? '运行中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggle(row)" size="small"
              style="--el-switch-on-color:#13ce66; --el-switch-off-color:#ff4949;" />
            <el-button size="small" type="danger" link @click="deleteTask(row)" style="margin-left:6px;">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { Clock, Refresh, Plus, Select } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api.js'

export default {
  components: { Clock, Refresh, Plus, Select },
  data() {
    return {
      tasks: [],
      submitting: false,
      formRef: null,
      form: { type: '', displayName: '', timeRangeCode: 'lastWeek', cron: '', description: '' },
      rules: {
        type: [
          { required: true, message: '请输入任务标识', trigger: 'blur' },
          { pattern: /^[a-zA-Z][a-zA-Z0-9_-]{0,29}$/, message: '字母开头，可包含字母/数字/-/_', trigger: 'blur' }
        ],
        displayName: [
          { required: true, message: '请输入显示名称', trigger: 'blur' }
        ],
        timeRangeCode: [
          { required: true, message: '请选择时间范围', trigger: 'change' }
        ],
        cron: [
          { required: true, message: '请输入 Cron 表达式', trigger: 'blur' },
          { pattern: /^(\S+\s+){4,6}\S+$/, message: 'Cron 格式不正确，请检查', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try {
        const d = await apiGet('/api/admin/schedules', { noRedirect: true })
        if (d.success) this.tasks = d.tasks
      } catch {}
    },
    async addTask() {
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) {
        ElMessage.warning('请检查表单中的错误提示')
        return
      }
      this.submitting = true
      try {
        const d = await apiPost('/api/admin/schedules', { ...this.form, enabled: true }, { noRedirect: true })
        if (d.success) {
          ElMessage.success('任务已创建')
          this.resetForm()
          this.load()
        } else {
          ElMessage.error(d.message)
        }
      } catch { ElMessage.error('请求失败') }
      this.submitting = false
    },
    resetForm() {
      this.form = { type: '', displayName: '', timeRangeCode: 'lastWeek', cron: '', description: '' }
      this.$refs.formRef?.clearValidate()
    },
    async toggle(t) {
      try {
        const d = await apiPost(`/api/admin/schedules/${t.type}/toggle`, {}, { noRedirect: true })
        if (!d.success) { this.load(); ElMessage.error(d.message) }
      } catch { this.load(); ElMessage.error('请求失败') }
    },
    async updateCron(t) {
      try {
        const d = await apiPut(`/api/admin/schedules/${t.type}`, { cron: t.cron, timeRangeCode: t.timeRangeCode, enabled: t.enabled }, { noRedirect: true })
        if (d.success) { this.load(); ElMessage.success('已更新') }
        else ElMessage.error('失败: ' + d.message)
      } catch { ElMessage.error('请求失败') }
    },
    async deleteTask(t) {
      try {
        await ElMessageBox.confirm(`确定删除任务「${t.displayName}」？`, '警告', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
        const d = await apiDelete(`/api/admin/schedules/${t.type}`, { noRedirect: true })
        if (d.success) { ElMessage.success('已删除'); this.load() }
        else ElMessage.error(d.message)
      } catch {}
    }
  }
}
</script>
