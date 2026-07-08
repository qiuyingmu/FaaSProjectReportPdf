<template>
  <div>
    <el-row :gutter="16" style="margin-bottom:16px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399; font-size:13px; margin-bottom:4px;">运行状态</div>
            <div :style="{ fontSize:'24px', fontWeight:700, color: healthColor }">{{ health }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399; font-size:13px; margin-bottom:4px;">日志文件</div>
            <div style="font-size:24px; font-weight:700; color:#409eff;">{{ logSize }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399; font-size:13px; margin-bottom:4px;">定时任务</div>
            <div style="font-size:24px; font-weight:700; color:#67c23a;">3 个</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399; font-size:13px; margin-bottom:4px;">应用端口</div>
            <div style="font-size:24px; font-weight:700; color:#e6a23c;">9001</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header><span style="font-weight:600;">系统信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="应用名称">report-springboot</el-descriptions-item>
        <el-descriptions-item label="运行端口">9001</el-descriptions-item>
        <el-descriptions-item label="日志路径">logs/report.log</el-descriptions-item>
        <el-descriptions-item label="定时任务">周报 / 月报 / 季报（可配置）</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { apiGet } from '../utils/api.js'

export default {
  data() {
    return { health: '检查中...', healthColor: '#909399', logSize: '-' }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try {
        const d = await apiGet('/actuator/health', { noRedirect: true })
        this.health = d.status === 'UP' ? '正常运行' : d.status
        this.healthColor = d.status === 'UP' ? '#67c23a' : '#f56c6c'
      } catch { this.health = '无法连接'; this.healthColor = '#f56c6c' }
      try {
        const d = await apiGet('/api/admin/logs?lines=1', { noRedirect: true })
        if (d.success) {
          const s = d.size
          this.logSize = s > 1048576 ? (s/1048576).toFixed(1)+' MB' : s > 1024 ? (s/1024).toFixed(0)+' KB' : s+' B'
        }
      } catch {}
    }
  }
}
</script>
