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
              <el-tag v-if="result.weekly.platform" :type="result.weekly.platform.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                平台: {{ result.weekly.platform.records || result.weekly.platform.message || result.weekly.platform.status }}条
              </el-tag>
              <el-tag v-if="result.weekly.project" :type="result.weekly.project.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                项目: {{ result.weekly.project.records || result.weekly.project.message || result.weekly.project.status }}条
              </el-tag>
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
              <el-tag v-if="result.monthly.platform" :type="result.monthly.platform.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                平台: {{ result.monthly.platform.records || result.monthly.platform.message || result.monthly.platform.status }}条
              </el-tag>
              <el-tag v-if="result.monthly.project" :type="result.monthly.project.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                项目: {{ result.monthly.project.records || result.monthly.project.message || result.monthly.project.status }}条
              </el-tag>
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
              <el-tag v-if="result.quarterly.platform" :type="result.quarterly.platform.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                平台: {{ result.quarterly.platform.records || result.quarterly.platform.message || result.quarterly.platform.status }}
              </el-tag>
              <el-tag v-if="result.quarterly.project" :type="result.quarterly.project.status === 'success' ? 'success' : 'warning'" size="small" style="margin:2px;">
                项目: {{ result.quarterly.project.records || result.quarterly.project.message || result.quarterly.project.status }}
              </el-tag>
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
      this.loading = period
      try {
        const d = await apiPost('/api/admin/reports/generate', { period }, { noRedirect: true })
        if (d.success) {
          this.result[period] = d.details
          ElMessage.success(`${d.label} 生成完成（${(d.cost / 1000).toFixed(1)}s）`)
        } else {
          ElMessage.error(d.message || '生成失败')
        }
      } catch (e) {
        ElMessage.error('请求失败')
      }
      this.loading = null
    }
  }
}
</script>
