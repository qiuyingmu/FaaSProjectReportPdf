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

    <!-- 筛选栏 -->
    <el-form :model="filters" inline size="small" style="margin-bottom:12px;">
      <el-form-item label="操作人">
        <el-input v-model="filters.operator" placeholder="模糊搜索" clearable
          style="width:140px;" @keyup.enter="loadLogs" />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="filters.action" placeholder="全部" clearable style="width:130px;">
          <el-option label="登录成功" value="LOGIN" />
          <el-option label="登录失败" value="LOGIN_FAIL" />
          <el-option label="生成报告" value="REPORT_GEN" />
          <el-option label="更新 Cron" value="SCHEDULE_UPDATE" />
          <el-option label="启停任务" value="SCHEDULE_TOGGLE" />
          <el-option label="系统错误" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="filters.result" placeholder="全部" clearable style="width:100px;">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
          <el-option label="已封锁" value="BLOCKED" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="filters.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width:320px;"
          :default-time="[new Date(2000,0,1,0,0,0), new Date(2000,11,31,23,59,59)]"
          @change="loadLogs" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadLogs">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetFilters">
          <el-icon><Delete /></el-icon> 重置
        </el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logs" border stripe style="width:100%;" size="small" v-loading="loading" :max-height="480">
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
import { List, Refresh, Search, Delete } from '@element-plus/icons-vue'
import { apiGet } from '../utils/api.js'

export default {
  components: { List, Refresh, Search, Delete },
  data() {
    return {
      logs: [],
      loading: false,
      page: 1,
      size: 20,
      total: 0,
      filters: {
        operator: '',
        action: '',
        result: '',
        dateRange: null
      }
    }
  },
  mounted() { this.loadLogs() },
  methods: {
    buildQuery() {
      const params = new URLSearchParams()
      params.set('page', this.page - 1)
      params.set('size', this.size)
      if (this.filters.operator) params.set('operator', this.filters.operator)
      if (this.filters.action) params.set('action', this.filters.action)
      if (this.filters.result) params.set('result', this.filters.result)
      if (this.filters.dateRange && this.filters.dateRange.length === 2) {
        params.set('startDate', this.filters.dateRange[0])
        params.set('endDate', this.filters.dateRange[1])
      }
      return params.toString()
    },
    async loadLogs() {
      this.loading = true
      this.page = 1
      try {
        const d = await apiGet(`/api/admin/operations?${this.buildQuery()}`, { noRedirect: true })
        if (d.success) { this.logs = d.logs; this.total = d.total }
      } catch {}
      this.loading = false
    },
    async onPageChange() {
      this.loading = true
      try {
        const d = await apiGet(`/api/admin/operations?${this.buildQuery()}`, { noRedirect: true })
        if (d.success) { this.logs = d.logs; this.total = d.total }
      } catch {}
      this.loading = false
    },
    resetFilters() {
      this.filters = { operator: '', action: '', result: '', dateRange: null }
      this.loadLogs()
    },
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
