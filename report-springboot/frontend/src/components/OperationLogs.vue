<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display:flex; align-items:center; justify-content:space-between;">
        <span style="font-weight:600;">
          <el-icon style="vertical-align:middle; margin-right:4px;"><List /></el-icon>
          操作日志
        </span>
        <el-button size="small" @click="loadLogs">
          <el-icon style="margin-right:4px;"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </template>

    <el-table :data="logs" border stripe style="width:100%;" size="small" v-loading="loading" :max-height="520">
      <el-table-column label="时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" width="90">
        <template #default="{ row }">{{ row.operator }}</template>
      </el-table-column>
      <el-table-column label="操作类型" width="130">
        <template #default="{ row }">
          <el-tag :type="tagType(row.action)" size="small" effect="plain">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="详情" min-width="200">
        <template #default="{ row }">{{ row.detail }}</template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.result === 'SUCCESS' ? 'success' : row.result === 'BLOCKED' ? 'danger' : 'warning'" size="small">
            {{ resultLabel(row.result) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="80">
        <template #default="{ row }">
          {{ row.durationMs ? row.durationMs + 'ms' : '-' }}
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > 0" style="display:flex; justify-content:center; margin-top:12px;">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="onPageChange"
        small />
    </div>
  </el-card>
</template>

<script>
import { List, Refresh } from '@element-plus/icons-vue'
import { apiGet } from '../utils/api.js'

export default {
  components: { List, Refresh },
  data() {
    return { logs: [], loading: false, page: 1, size: 20, total: 0 }
  },
  mounted() { this.loadLogs() },
  methods: {
    async loadLogs() {
      this.loading = true
      try {
        const d = await apiGet(`/api/admin/operations?page=${this.page - 1}&size=${this.size}`, { noRedirect: true })
        if (d.success) { this.logs = d.logs; this.total = d.total }
      } catch {}
      this.loading = false
    },
    onPageChange() { this.loadLogs() },
    formatTime(t) {
      if (!t) return '-'
      return t.substring(0, 19).replace('T', ' ')
    },
    tagType(action) {
      if (action === 'LOGIN' || action === 'LOGIN_FAIL') return ''
      if (action === 'SCHEDULE_UPDATE') return 'warning'
      if (action === 'SCHEDULE_TOGGLE') return 'info'
      if (action === 'REPORT_GEN') return 'success'
      return ''
    },
    actionLabel(action) {
      const map = {
        LOGIN: '登录成功',
        LOGIN_FAIL: '登录失败',
        SCHEDULE_UPDATE: '更新 Cron',
        SCHEDULE_TOGGLE: '启停任务',
        REPORT_GEN: '生成报告',
        ERROR: '系统错误'
      }
      return map[action] || action
    },
    resultLabel(r) {
      const map = { SUCCESS: '成功', FAILURE: '失败', BLOCKED: '已封锁' }
      return map[r] || r
    }
  }
}
</script>
