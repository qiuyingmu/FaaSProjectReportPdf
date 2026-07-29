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
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="Key" min-width="200">
        <template #default="{ row }">
          <code style="background:#f5f7fa; padding:2px 8px; border-radius:4px; font-size:12px;">{{ row.keyPrefix }}</code>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总调用" width="100" sortable :sort-method="(a,b)=>a.totalRequests-b.totalRequests">
        <template #default="{ row }">
          <span style="font-weight:600; color:#3b82f6;">{{ row.totalRequests }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="最后使用" width="160">
        <template #default="{ row }">{{ row.lastUsedAt ? formatTime(row.lastUsedAt) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="showStats(row)">
            <el-icon><DataLine /></el-icon>统计
          </el-button>
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

    <!-- 统计弹窗 -->
    <el-dialog v-model="showStatsDialog" :title="statsTitle" width="700px">
      <div class="stats-summary">
        <div class="stat-item">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ statsTotal }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">统计区间</div>
          <div class="stat-value" style="font-size:14px;">{{ statsRange }}</div>
        </div>
      </div>
      <v-chart class="chart" :option="chartOption" autoresize />
    </el-dialog>
  </div>
</template>

<script>
import { Plus, DataLine } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api.js'

// 注册 ECharts 组件
use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

export default {
  name: 'ApiKeyManager',
  components: { Plus, DataLine, VChart },
  data() {
    return {
      keys: [],
      loading: false,
      creating: false,
      showCreateDialog: false,
      showFullKeyDialog: false,
      newName: '',
      createdKey: '',
      // 统计弹窗
      showStatsDialog: false,
      statsTitle: '',
      statsRange: '',
      statsTotal: 0,
      chartOption: { tooltip: { trigger: 'axis' }, xAxis: { type: 'category' }, yAxis: { type: 'value' }, series: [] }
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
        await ElMessageBox.confirm(`确认删除 Key "${row.name}"？删除后该 Key 立即失效，统计记录也会被清除。`, '删除确认', {
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
    async showStats(row) {
      // 默认 30 天
      const to = new Date()
      const from = new Date()
      from.setDate(to.getDate() - 29)
      const fmt = d => d.toISOString().slice(0, 10)
      const fromStr = fmt(from)
      const toStr = fmt(to)

      try {
        const data = await apiGet(`/api/admin/api-keys/${row.id}/stats?from=${fromStr}&to=${toStr}`)
        const daily = data.daily || []

        this.statsTitle = `${row.name} - 调用统计`
        this.statsRange = `${fromStr} 至 ${toStr}`
        this.statsTotal = daily.reduce((s, d) => s + d.count, 0)

        this.chartOption = {
          tooltip: {
            trigger: 'axis',
            formatter: p => `${p[0].axisValue}<br/>请求次数: <strong>${p[0].data}</strong>`
          },
          grid: { left: 50, right: 20, top: 20, bottom: 30 },
          xAxis: {
            type: 'category',
            data: daily.map(d => d.date.slice(5)),
            boundaryGap: false,
            axisLine: { lineStyle: { color: '#dcdfe6' } },
            axisLabel: { color: '#909399', fontSize: 11 }
          },
          yAxis: {
            type: 'value',
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } },
            axisLabel: { color: '#909399', fontSize: 11 }
          },
          series: [{
            name: '请求次数',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            data: daily.map(d => d.count),
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.4)' }, { offset: 1, color: 'rgba(59,130,246,0)' }]
              }
            },
            lineStyle: { color: '#3b82f6', width: 2 },
            itemStyle: { color: '#3b82f6' }
          }]
        }
        this.showStatsDialog = true
      } catch (e) {
        ElMessage.error('加载统计失败')
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
.stats-summary { display:flex; gap:32px; padding:8px 0 16px; border-bottom:1px solid #f0f2f5; margin-bottom:12px; }
.stat-item { flex:1; }
.stat-label { font-size:12px; color:#909399; margin-bottom:4px; }
.stat-value { font-size:28px; font-weight:600; color:#3b82f6; }
.chart { height: 320px; }
</style>