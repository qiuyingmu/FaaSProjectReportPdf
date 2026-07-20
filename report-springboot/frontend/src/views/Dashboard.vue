<template>
  <el-container style="min-height:100vh;">
    <el-header style="background:#1a1a2e; display:flex; align-items:center; justify-content:space-between; padding:0 24px;">
      <h1 style="color:#fff; font-size:16px; margin:0;">
        <el-icon size="20" style="vertical-align:middle; margin-right:6px;"><DataAnalysis /></el-icon>
        运营报告管理后台
      </h1>
      <el-dropdown trigger="click" @command="handleCommand">
        <span style="color:rgba(255,255,255,0.8); cursor:pointer; display:flex; align-items:center; gap:6px;">
          <el-icon><UserFilled /></el-icon>{{ username }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>

    <el-container style="height:calc(100vh - 60px);">
      <el-aside width="200px" style="background:#fff; border-right:1px solid #e4e7ed;">
        <el-menu :default-active="activeTab" @select="activeTab = $event" style="border:none;">
          <el-menu-item index="overview">
            <el-icon><Monitor /></el-icon><span>系统概览</span>
          </el-menu-item>
          <el-menu-item index="logs">
            <el-icon><Document /></el-icon><span>实时日志</span>
          </el-menu-item>
          <el-menu-item index="operations">
            <el-icon><List /></el-icon><span>操作日志</span>
          </el-menu-item>
          <el-menu-item index="generate">
            <el-icon><Pointer /></el-icon><span>手动生成</span>
          </el-menu-item>
          <el-menu-item index="schedule">
            <el-icon><Clock /></el-icon><span>定时任务</span>
          </el-menu-item>
          <el-menu-item index="users">
            <el-icon><User /></el-icon><span>管理员</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main style="background:#f0f2f5; padding:20px;">
        <SystemOverview v-show="activeTab === 'overview'" />
        <LogViewer v-show="activeTab === 'logs'" />
        <OperationLogs v-show="activeTab === 'operations'" />
        <ReportGenerator v-show="activeTab === 'generate'" />
        <ScheduleManager v-show="activeTab === 'schedule'" />
        <UserManager v-show="activeTab === 'users'" :currentUser="username" />
      </el-main>
    </el-container>
  </el-container>

  <!-- 页脚：ICP + 公安备案 -->
  <el-footer style="background:#1a1a2e; color:rgba(255,255,255,0.5); text-align:center; line-height:40px; font-size:12px;">
    <a href="https://beian.miit.gov.cn/" target="_blank" style="color:rgba(255,255,255,0.5); text-decoration:none;">黔ICP备19003726号-1</a>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=52011502002024" target="_blank" style="color:rgba(255,255,255,0.5); text-decoration:none;">
      <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAACXBIWXMAAAsTAAALEwEAmpwYAAABWWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyI+CiAgICAgICAgIDx0aWZmOkNvbXByZXNzaW9uPjE8L3RpZmY6Q29tcHJlc3Npb24+CiAgICAgICAgIDx0aWZmOlBPcmlkZW50YXRpb24+MTwvdGlmZjpQb3JpZW50YXRpb24+CiAgICAgIDwvcmRmOkRlc2NyaXB0aW9uPgogICA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgwldOnJAAAAXElEQVQ4EWNg+M9AAWBigALKABPAJ4EVYGJiYqBME4LhBQsWMEA1MTAwMFBkCLIAJiYmBopMGUAGIFmBpQHZCv5TbACyCxgYGCj2ApYwQBkITKA2YCsFKBUBsgBz8RqFgAes+QAAAABJRU5ErkJggg==" style="vertical-align:middle; margin-right:4px;" />
      贵公网安备 52011502002024号
    </a>
    &nbsp;&nbsp;|&nbsp;&nbsp;<span>贵州建工监理咨询有限公司</span>
  </el-footer>
</template>

<script>
import { DataAnalysis, Monitor, Document, List, Pointer, Clock, UserFilled, ArrowDown, SwitchButton, User } from '@element-plus/icons-vue'
import SystemOverview from '../components/SystemOverview.vue'
import LogViewer from '../components/LogViewer.vue'
import OperationLogs from '../components/OperationLogs.vue'
import ReportGenerator from '../components/ReportGenerator.vue'
import ScheduleManager from '../components/ScheduleManager.vue'
import UserManager from '../components/UserManager.vue'
import { apiGet, apiFetch } from '../utils/api.js'

export default {
  components: { SystemOverview, LogViewer, OperationLogs, ReportGenerator, ScheduleManager, UserManager, DataAnalysis, Monitor, Document, List, Pointer, Clock, UserFilled, ArrowDown, SwitchButton, User },
  data() {
    return { username: 'admin', activeTab: 'overview' }
  },
  mounted() {
    this.loadUsername()
  },
  methods: {
    async loadUsername() {
      try {
        const d = await apiGet('/api/admin/session')
        if (d.valid) this.username = d.username
      } catch {}
    },
    async handleCommand(cmd) {
      if (cmd === 'logout') {
        try {
          await apiFetch('/admin/logout', { method: 'POST' })
        } catch {}
        this.$router.push('/login')
      }
    }
  }
}
</script>
