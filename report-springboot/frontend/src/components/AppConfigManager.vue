<template>
  <div class="app-config-manager">
    <div class="header-bar">
      <h2 style="margin:0;">宜搭应用配置</h2>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon>新增配置
      </el-button>
    </div>

    <p style="color:#909399; margin:8px 0 16px; font-size:13px;">
      配置宜搭应用凭证（appType / systemToken / userId）。留空则使用 .env 中的既有配置；保存后优先使用数据库配置。
    </p>

    <el-table :data="configs" stripe v-loading="loading">
      <el-table-column prop="configKey" label="标识" width="140" />
      <el-table-column prop="appType" label="应用编码 appType" min-width="180" show-overflow-tooltip />
      <el-table-column label="系统 Token" min-width="160">
        <template #default="{ row }">
          <el-tag type="info" size="small" effect="plain">
            {{ row.systemToken ? '已配置（不显示）' : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="操作人 userId" width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '编辑应用配置' : '新增应用配置'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标识 configKey" required>
          <el-input v-model="form.configKey" placeholder="如 production" :disabled="!!editing" />
        </el-form-item>
        <el-form-item label="应用编码" required>
          <el-input v-model="form.appType" placeholder="APP_xxxx" />
        </el-form-item>
        <el-form-item :label="editing ? '系统 Token' : '系统 Token'" :required="!editing">
          <el-input v-model="form.systemToken"
                    :placeholder="editing ? '留空表示不修改（凭证不回显）' : '宜搭系统 Token'"
                    show-password />
        </el-form-item>
        <el-form-item label="操作人 ID" required>
          <el-input v-model="form.userId" placeholder="钉钉 userId" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiDelete } from '../utils/api.js'

export default {
  name: 'AppConfigManager',
  components: { Plus },
  data() {
    return {
      configs: [],
      loading: false,
      saving: false,
      showDialog: false,
      editing: null,
      form: { configKey: '', appType: '', systemToken: '', userId: '', enabled: true }
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        this.configs = await apiGet('/api/admin/app-configs')
      } catch (e) {
        ElMessage.error('加载失败')
      }
      this.loading = false
    },
    openEdit(row) {
      this.editing = row
      this.form = row ? { ...row } : { configKey: '', appType: '', systemToken: '', userId: '', enabled: true }
      this.showDialog = true
    },
    async save() {
      // 新增时 systemToken 必填；编辑时留空 = 不修改（后端保留原值）
      if (!this.form.configKey || !this.form.appType || !this.form.userId) {
        ElMessage.warning('请填写必填项')
        return
      }
      if (!this.editing && !this.form.systemToken) {
        ElMessage.warning('新增配置必须填写系统 Token')
        return
      }
      this.saving = true
      try {
        await apiPost('/api/admin/app-configs', this.form)
        ElMessage.success('已保存')
        this.showDialog = false
        await this.load()
      } catch (e) {
        ElMessage.error('保存失败')
      }
      this.saving = false
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm(`确认删除应用配置 "${row.configKey}"？`, '删除确认', { type: 'warning' })
        await apiDelete(`/api/admin/app-configs/${row.id}`)
        ElMessage.success('已删除')
        await this.load()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
      }
    }
  }
}
</script>

<style scoped>
.app-config-manager { background:#fff; padding:20px; border-radius:8px; }
.header-bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
</style>
