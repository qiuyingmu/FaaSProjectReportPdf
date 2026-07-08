<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display:flex; align-items:center; justify-content:space-between;">
        <span style="font-weight:600;">
          <el-icon style="vertical-align:middle; margin-right:4px;"><Document /></el-icon>
          实时日志
        </span>
        <el-tag type="info" size="small">{{ count }} 行</el-tag>
      </div>
    </template>

    <div style="display:flex; gap:12px; align-items:center; margin-bottom:12px; flex-wrap:wrap;">
      <span style="font-size:13px; color:#909399;">显示行数</span>
      <el-select v-model="lineCount" size="small" style="width:100px;">
        <el-option :value="100" label="100" />
        <el-option :value="200" label="200" />
        <el-option :value="500" label="500" />
        <el-option :value="1000" label="1000" />
      </el-select>
      <el-button size="small" type="primary" @click="loadLogs">
        <el-icon style="margin-right:4px;"><Refresh /></el-icon>刷新
      </el-button>
      <el-switch v-model="autoRefresh" active-text="自动刷新" size="small" />
    </div>

    <div ref="viewer" style="
      background:#1e1e2e; color:#cdd6f4; font-family:'JetBrains Mono','Consolas',monospace;
      font-size:12px; line-height:1.7; padding:16px; border-radius:6px;
      max-height:580px; overflow-y:auto; white-space:pre-wrap; word-break:break-all;">
      <div v-for="(line, i) in lines" :key="i" :style="lineStyle(line)">{{ line }}</div>
      <div v-if="lines.length === 0" style="text-align:center; color:#6c7086; padding:20px;">暂无日志</div>
    </div>
  </el-card>
</template>

<script>
import { Document, Refresh } from '@element-plus/icons-vue'
import { apiGet } from '../utils/api.js'

export default {
  components: { Document, Refresh },
  data() {
    return { lines: [], lineCount: 200, autoRefresh: true, timer: null, count: 0 }
  },
  mounted() { this.loadLogs(); this.startAuto() },
  watch: { autoRefresh(v) { v ? this.startAuto() : this.stopAuto() } },
  methods: {
    async loadLogs() {
      try {
        const d = await apiGet('/api/admin/logs?lines=' + this.lineCount, { noRedirect: true })
        if (d.success) { this.lines = d.lines; this.count = d.count }
        this.$nextTick(() => { if (this.$refs.viewer) this.$refs.viewer.scrollTop = this.$refs.viewer.scrollHeight })
      } catch { this.lines = ['❌ 加载失败'] }
    },
    lineStyle(line) {
      if (!line) return { padding:'1px 0' }
      if (line.includes('ERROR') || line.includes('❌')) return { color:'#f38ba8', padding:'1px 0' }
      if (line.includes('WARN') || line.includes('⚠')) return { color:'#fab387', padding:'1px 0' }
      return { color:'#cdd6f4', padding:'1px 0' }
    },
    startAuto() { this.stopAuto(); this.timer = setInterval(() => this.loadLogs(), 10000) },
    stopAuto() { if (this.timer) { clearInterval(this.timer); this.timer = null } }
  },
  beforeUnmount() { this.stopAuto() }
}
</script>
