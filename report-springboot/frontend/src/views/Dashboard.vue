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
        <SystemOverview v-if="activeTab === 'overview'" />
        <LogViewer v-if="activeTab === 'logs'" />
        <OperationLogs v-if="activeTab === 'operations'" />
        <ReportGenerator v-if="activeTab === 'generate'" />
        <ScheduleManager v-if="activeTab === 'schedule'" />
        <UserManager v-if="activeTab === 'users'" :currentUser="username" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { DataAnalysis, Monitor, Document, List, Pointer, Clock, UserFilled, ArrowDown, SwitchButton, User } from '@element-plus/icons-vue'
import SystemOverview from '../components/SystemOverview.vue'
import LogViewer from '../components/LogViewer.vue'
import OperationLogs from '../components/OperationLogs.vue'
import ReportGenerator from '../components/ReportGenerator.vue'
import ScheduleManager from '../components/ScheduleManager.vue'
import UserManager from '../components/UserManager.vue'

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
        const res = await fetch('/api/admin/session', { credentials: 'include' })
        const d = await res.json()
        if (d.valid) this.username = d.username
      } catch {}
    },
    async handleCommand(cmd) {
      if (cmd === 'logout') {
        try {
          await fetch('/admin/logout', { method: 'POST', credentials: 'include' })
        } catch {}
        this.$router.push('/login')
      }
    }
  }
}
</script>
