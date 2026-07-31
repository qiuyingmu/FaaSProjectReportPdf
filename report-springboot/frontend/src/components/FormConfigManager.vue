<template>
  <div class="form-config-manager">
    <div class="header-bar">
      <h2 style="margin:0;">表单配置</h2>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon>新增数据源
      </el-button>
    </div>

    <p style="color:#909399; margin:8px 0 16px; font-size:13px;">
      配置报表统计的数据源表单。默认配置已自动初始化，可在此增删改（改后下次生成报表生效）。
    </p>

    <el-table :data="configs" stripe v-loading="loading">
      <el-table-column prop="sortOrder" label="排序" width="60" />
      <el-table-column prop="label" label="名称" width="120" />
      <el-table-column label="颜色" width="80">
        <template #default="{ row }">
          <span :style="{ display:'inline-block', width:'16px', height:'16px', borderRadius:'4px', background: row.color, verticalAlign:'middle' }"></span>
        </template>
      </el-table-column>
      <el-table-column prop="configKey" label="标识" width="100" />
      <el-table-column prop="formUuid" label="表单 UUID" min-width="220" show-overflow-tooltip />
      <el-table-column prop="personField" label="人员字段" width="130" show-overflow-tooltip />
      <el-table-column prop="dateField" label="日期字段" width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '编辑数据源' : '新增数据源'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标识 configKey" required>
          <el-input v-model="form.configKey" placeholder="唯一标识，如 docLib" :disabled="!!editing" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.label" placeholder="如 资料库" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="表单 UUID" required>
          <el-input v-model="form.formUuid" placeholder="FORM-xxxx" />
        </el-form-item>
        <el-form-item label="人员字段" required>
          <el-input v-model="form.personField" placeholder="textField_xxx" />
        </el-form-item>
        <el-form-item label="日期字段" required>
          <el-input v-model="form.dateField" placeholder="dateField_xxx" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="99" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, apiDelete } from '../utils/api.js'

export default {
  name: 'FormConfigManager',
  components: { Plus },
  data() {
    return {
      configs: [],
      loading: false,
      saving: false,
      showDialog: false,
      editing: null,
      form: { configKey: '', label: '', color: '#3b82f6', formUuid: '', personField: '', dateField: '', sortOrder: 0, enabled: true }
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        this.configs = await apiGet('/api/admin/form-configs')
      } catch (e) {
        ElMessage.error('加载失败')
      }
      this.loading = false
    },
    openEdit(row) {
      this.editing = row
      if (row) {
        this.form = { ...row }
      } else {
        this.form = { configKey: '', label: '', color: '#3b82f6', formUuid: '', personField: '', dateField: '', sortOrder: this.configs.length, enabled: true }
      }
      this.showDialog = true
    },
    async save() {
      if (!this.form.configKey || !this.form.label || !this.form.formUuid || !this.form.personField || !this.form.dateField) {
        ElMessage.warning('请填写必填项')
        return
      }
      this.saving = true
      try {
        await apiPost('/api/admin/form-configs', this.form)
        ElMessage.success('已保存')
        this.showDialog = false
        await this.load()
      } catch (e) {
        ElMessage.error('保存失败')
      }
      this.saving = false
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm(`确认删除数据源 "${row.label}"？`, '删除确认', { type: 'warning' })
        await apiDelete(`/api/admin/form-configs/${row.id}`)
        ElMessage.success('已删除')
        await this.load()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
      }
    }
  }
}
</script>

<style scoped>
.form-config-manager { background:#fff; padding:20px; border-radius:8px; }
.header-bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
</style>
