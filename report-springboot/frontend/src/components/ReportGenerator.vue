<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <span style="font-weight:600;">
            手动生成报告
          </span>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '24px' }">
            <div style="font-size:40px; margin-bottom:12px;">📅</div>
            <div style="font-weight:600; font-size:16px; margin-bottom:4px;">周报</div>
            <div style="color:#909399; font-size:12px; margin-bottom:16px;">生成上周平台报告 + 项目报告</div>
            <el-button type="primary" :loading="loading === 'weekly'" @click="generate('weekly')" round>
              {{ loading === 'weekly' ? '生成中...' : '立即生成' }}
            </el-button>
            <div v-if="result.weekly" style="margin-top:12px; font-size:12px;">
              <el-tag type="success" size="small" style="margin:2px; cursor:pointer;" @click="openUrl(result.weekly.obsUrl)">
                查看报告 ⏏
              </el-tag>
              <div style="color:#909399; margin-top:4px;">{{ result.weekly.reportName }} ({{ formatSize(result.weekly.pdfSize) }})</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '24px' }">
            <div style="font-size:40px; margin-bottom:12px;">📆</div>
            <div style="font-weight:600; font-size:16px; margin-bottom:4px;">月报</div>
            <div style="color:#909399; font-size:12px; margin-bottom:16px;">生成上月平台报告 + 项目报告</div>
            <el-button type="warning" :loading="loading === 'monthly'" @click="generate('monthly')" round>
              {{ loading === 'monthly' ? '生成中...' : '立即生成' }}
            </el-button>
            <div v-if="result.monthly" style="margin-top:12px; font-size:12px;">
              <el-tag type="success" size="small" style="margin:2px; cursor:pointer;" @click="openUrl(result.monthly.obsUrl)">
                查看报告 ⏏
              </el-tag>
              <div style="color:#909399; margin-top:4px;">{{ result.monthly.reportName }} ({{ formatSize(result.monthly.pdfSize) }})</div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '24px' }">
            <div style="font-size:40px; margin-bottom:12px;">🗓️</div>
            <div style="font-weight:600; font-size:16px; margin-bottom:4px;">季报</div>
            <div style="color:#909399; font-size:12px; margin-bottom:16px;">生成上季度平台报告 + 项目报告</div>
            <el-button type="success" :loading="loading === 'quarterly'" @click="generate('quarterly')" round>
              {{ loading === 'quarterly' ? '生成中...' : '立即生成' }}
            </el-button>
            <div v-if="result.quarterly" style="margin-top:12px; font-size:12px;">
              <el-tag type="success" size="small" style="margin:2px; cursor:pointer;" @click="openUrl(result.quarterly.obsUrl)">
                查看报告 ⏏
              </el-tag>
              <div style="color:#909399; margin-top:4px;">{{ result.quarterly.reportName }} ({{ formatSize(result.quarterly.pdfSize) }})</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { apiPost } from '../utils/api.js'

export default {
  data() {
    return { loading: null, result: {} }
  },
  methods: {
    async generate(period) {
      if (this.loading) {
        ElMessage.warning('已有生成任务进行中，请稍候')
        return
      }
      this.loading = period
      const startTime = Date.now()
      try {
        const d = await apiPost('/api/admin/reports/generate', { period }, { noRedirect: true })
        if (d.success) {
          this.result[period] = {
            reportName: d.reportName,
            obsUrl: d.obsUrl,
            pdfSize: d.pdfSize
          }
          ElMessage.success(`${d.label} 生成完成（${(d.cost / 1000).toFixed(1)}s）`)
        } else {
          ElMessage.error(d.message || '生成失败')
        }
      } catch (e) {
        const elapsed = ((Date.now() - startTime) / 1000).toFixed(0)
        if (e.name === 'AbortError') {
          ElMessage.error(`生成超时（${elapsed}s），大数据量月报请稍后重试`)
        } else {
          ElMessage.error(`请求失败（${elapsed}s）`)
        }
      }
      this.loading = null
    },
    formatSize(bytes) {
      if (!bytes || bytes <= 0) return '本地生成'
      return (bytes / 1024).toFixed(0) + 'KB'
    },
    openUrl(url) {
      if (url) window.open(url, '_blank')
    }
  }
}
</script>
