<template>
  <div class="api-key-manager">
    <div class="header-bar">
      <h2 style="margin:0;">API Key 管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>创建 API Key
      </el-button>
    </div>

    <p style="color:#909399; margin:8px 0 16px; font-size:13px;">
      供宜搭 HTTP 连接器等外部系统调用我方接口使用。每个 Key 在创建时显示完整值一次，之后只能查看前缀。
    </p>

    <el-table :data="keys" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="Key" min-width="220">
        <template #default="{ row }">
          <code style="background:#f5f7fa; padding:2px 8px; border-radius:4px; font-size:12px;">{{ row.keyPrefix }}</code>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="最后使用" width="170">
        <template #default="{ row }">{{ row.lastUsedAt ? formatTime(row.lastUsedAt) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggle(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建新 API Key" width="500px">
      <el-form @submit.prevent="handleCreate">
        <el-form-item label="名称" required>
          <el-input v-model="newName" placeholder="如：宜搭审批记录连接器" />
        </el-form-item>
        <p style="color:#909399; font-size:12px;">Key 创建后明文仅显示一次，请妥善保存。</p>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建成功展示弹窗 -->
    <el-dialog v-model="showFullKeyDialog" title="✅ 创建成功 - 请立即保存" width="520px" :close-on-click-modal="false" :close-on-press-escape="false">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px;">
        <strong>Key 明文仅显示一次！</strong>请立即复制并妥善保存，关闭此弹窗后将无法再次查看。
      </el-alert>
      <div style="background:#f5f7fa; padding:12px; border-radius:4px; word-break:break-all; font-family:monospace; font-size:13px; user-select:all;">
        {{ createdKey }}
      </div>
      <template #footer>
        <el-button type="primary" @click="copyKey">复制</el-button>
        <el-button @click="showFullKeyDialog = false">已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api.js'

export default {
  name: 'ApiKeyManager',
  components: { Plus },
  data() {
    return {
      keys: [],
      loading: false,
      creating: false,
      showCreateDialog: false,
      showFullKeyDialog: false,
      newName: '',
      createdKey: ''
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        this.keys = await apiGet('/api/admin/api-keys')
      } catch (e) {
        ElMessage.error('加载失败')
      }
      this.loading = false
    },
    async handleCreate() {
      if (!this.newName.trim()) {
        ElMessage.warning('请输入名称')
        return
      }
      this.creating = true
      try {
        const result = await apiPost('/api/admin/api-keys', { name: this.newName.trim() })
        this.showCreateDialog = false
        this.newName = ''
        this.createdKey = result.fullKey
        this.showFullKeyDialog = true
        await this.load()
      } catch (e) {
        ElMessage.error('创建失败')
      }
      this.creating = false
    },
    async toggle(row) {
      try {
        await apiPut(`/api/admin/api-keys/${row.id}/enabled`, { enabled: !row.enabled })
        ElMessage.success(row.enabled ? '已禁用' : '已启用')
        await this.load()
      } catch (e) {
        ElMessage.error('操作失败')
      }
    },
    async confirmDelete(row) {
      try {
        await ElMessageBox.confirm(`确认删除 Key "${row.name}"？删除后该 Key 立即失效。`, '删除确认', {
          type: 'warning',
          confirmButtonText: '删除',
          cancelButtonText: '取消'
        })
        await apiDelete(`/api/admin/api-keys/${row.id}`)
        ElMessage.success('已删除')
        await this.load()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
      }
    },
    async copyKey() {
      try {
        await navigator.clipboard.writeText(this.createdKey)
        ElMessage.success('已复制到剪贴板')
      } catch (e) {
        ElMessage.warning('复制失败，请手动选择复制')
      }
    },
    formatTime(s) {
      if (!s) return ''
      const d = new Date(s)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.api-key-manager { background:#fff; padding:20px; border-radius:8px; }
.header-bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
</style>