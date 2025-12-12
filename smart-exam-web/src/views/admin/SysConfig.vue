<template>
  <div class="sys-config-container">
    <div class="config-wrapper">
      <!-- 标题 -->
      <div class="page-header">
        <h2 class="page-title">系统设置</h2>
        <p class="page-subtitle">管理系统全局参数及第三方服务配置。</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading loading-icon"><Loading /></el-icon>
        <p class="loading-text">正在加载配置...</p>
      </div>

      <!-- 动态配置表单 -->
      <el-form
          v-else
          ref="formRef"
          :model="formModel"
          label-position="top"
          class="config-form"
      >
        <!-- 分组：Dify AI 配置 -->
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-header-content">
              <span class="card-title">Dify AI 服务配置</span>
              <el-tag size="small" type="info" effect="plain" round>核心</el-tag>
            </div>
          </template>

          <div class="form-grid">
            <template v-for="item in difyConfigs" :key="item.id">
              <el-form-item
                  :label="item.description || item.configKey"
                  class="form-item"
                  :class="{ 'full-width-item': isLongField(item.configKey) }"
              >
                <el-input
                    v-model="item.configValue"
                    :placeholder="'请输入 ' + (item.description || item.configKey)"
                    :type="isPasswordField(item.configKey) ? 'password' : 'text'"
                    :show-password="isPasswordField(item.configKey)"
                    size="large"
                >
                  <template #prefix>
                    <el-icon class="input-icon"><component :is="getIcon(item.configKey)" /></el-icon>
                  </template>
                </el-input>
                <!-- 提示信息 -->
                <div v-if="item.configKey === 'dify_base_url'" class="helper-text">
                  <el-icon><InfoFilled /></el-icon> Dify API 的服务地址，例如：https://api.dify.ai/v1
                </div>
              </el-form-item>
            </template>
          </div>
        </el-card>

        <!-- 分组：其他配置 -->
        <el-card v-if="otherConfigs.length > 0" shadow="never" class="config-card">
          <template #header>
            <div class="card-header-content other-config">
              <span class="card-title">其他配置</span>
            </div>
          </template>
          <div class="form-grid">
            <template v-for="item in otherConfigs" :key="item.id">
              <el-form-item :label="item.description || item.configKey" class="form-item">
                <el-input v-model="item.configValue" size="large" />
              </el-form-item>
            </template>
          </div>
        </el-card>

        <!-- 操作按钮 (悬浮底部) -->
        <div class="action-bar">
          <div class="action-buttons">
            <el-button @click="fetchConfig" :icon="Refresh" size="large">重置更改</el-button>
            <el-button type="primary" color="#6366f1" @click="handleSubmit" :loading="submitLoading" :icon="Check" size="large">
              保存配置
            </el-button>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Link, Key, Collection, Refresh, Check, Setting, InfoFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ConfigItem {
  id: number
  configKey: string
  configValue: string
  description: string
  createTime?: string
  updateTime?: string
}

const loading = ref(true)
const submitLoading = ref(false)
const configList = ref<ConfigItem[]>([])
const formModel = reactive({})

const difyConfigs = computed(() => configList.value.filter(item => item.configKey.startsWith('dify_')))
const otherConfigs = computed(() => configList.value.filter(item => !item.configKey.startsWith('dify_')))

const isPasswordField = (key: string) => {
  return key.includes('key') || key.includes('secret') || key.includes('password')
}

const isLongField = (key: string) => {
  return key === 'dify_base_url'
}

const getIcon = (key: string) => {
  if (key.includes('base_url')) return Link
  if (key.includes('dataset')) return Collection
  if (isPasswordField(key)) return Key
  return Setting
}

const fetchConfig = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/config')
    configList.value = res
  } catch (error) {
    console.error('获取配置失败', error)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    await request.put('/admin/config', configList.value)
    ElMessage.success('配置保存成功')
    await fetchConfig()
  } catch (error) {
    // error handled
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
/* 核心布局样式 - 替代 Tailwind Utility */
.sys-config-container {
  padding: 24px;
  background-color: #f9fafb; /* gray-50 */
  min-height: 100vh;
  box-sizing: border-box;
}

.config-wrapper {
  max-width: 900px; /* max-w-4xl */
  margin: 0 auto; /* mx-auto */
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937; /* gray-800 */
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280; /* gray-500 */
  margin: 0;
}

.loading-state {
  padding: 80px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  font-size: 36px;
  color: #6366f1; /* indigo-500 */
  margin-bottom: 16px;
}

.loading-text {
  color: #9ca3af; /* gray-400 */
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 32px; /* space-y-8 */
}

.config-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb; /* border-0 in Tailwind context usually relies on shadow, but explicit border is safer */
  overflow: visible;
}

.card-header-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 12px;
  border-left: 4px solid #6366f1; /* indigo-500 */
}

.card-header-content.other-config {
  border-left-color: #9ca3af; /* gray-400 */
}

.card-title {
  font-weight: 700;
  color: #374151; /* gray-700 */
  font-size: 18px;
}

/* Grid Layout for Form Items */
.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 768px) {
  .form-grid {
    grid-template-columns: repeat(2, 1fr);
    column-gap: 32px;
  }
}

.form-item {
  grid-column: span 1;
}

.full-width-item {
  grid-column: span 1;
}

@media (min-width: 768px) {
  .full-width-item {
    grid-column: span 2;
  }
}

.helper-text {
  font-size: 12px;
  color: #9ca3af; /* gray-400 */
  margin-top: 8px;
  margin-left: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.input-icon {
  color: #9ca3af;
}

/* Action Bar */
.action-bar {
  position: sticky;
  bottom: 24px;
  z-index: 20;
  display: flex;
  justify-content: center;
  pointer-events: none; /* Let clicks pass through empty space */
}

.action-buttons {
  pointer-events: auto;
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  padding: 12px 32px;
  border-radius: 9999px; /* full rounded */
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid #f3f4f6;
  display: flex;
  gap: 16px;
  transition: transform 0.3s;
}

.action-buttons:hover {
  transform: scale(1.05);
}

/* Element Plus Overrides */
:deep(.el-card__header) {
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
}

:deep(.el-card__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #6366f1 inset !important;
}
</style>