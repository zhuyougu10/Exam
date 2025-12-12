<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)] flex flex-col">
    <!-- 标题与描述 -->
    <div class="mb-6 shrink-0">
      <h2 class="text-2xl font-bold text-gray-800">系统设置</h2>
      <p class="text-gray-500 text-sm mt-1">管理系统全局参数及 AI 服务配置 (所有更改立即生效)</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex-1 flex justify-center items-center">
      <div class="text-center">
        <el-icon class="is-loading text-4xl text-indigo-500 mb-4"><Loading /></el-icon>
        <p class="text-gray-400">正在加载配置信息...</p>
      </div>
    </div>

    <!-- 动态配置表单 -->
    <el-form
        v-else
        ref="formRef"
        :model="formModel"
        label-position="top"
        class="flex-1 flex flex-col"
    >
      <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-6">
        <!-- 分组：Dify AI 配置 -->
        <el-card shadow="never" class="border-0 rounded-xl mb-6">
          <template #header>
            <div class="flex items-center gap-2 border-l-4 pl-3 py-1" style="border-color: #6366f1">
              <span class="font-bold text-gray-800 text-lg">AI 服务集成 (Dify)</span>
              <el-tag size="small" type="primary" effect="plain">Core</el-tag>
            </div>
          </template>

          <!-- Grid 布局：大屏显示 3 列，超大屏显示 4 列 -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            <template v-for="item in difyConfigs" :key="item.id">
              <el-form-item
                  :label="item.description || item.configKey"
                  class="col-span-1"
                  :class="{ 'md:col-span-2 lg:col-span-2': isLongField(item.configKey) }"
              >
                <el-input
                    v-model="item.configValue"
                    :placeholder="'请输入 ' + (item.description || item.configKey)"
                    :type="isPasswordField(item.configKey) ? 'password' : 'text'"
                    :show-password="isPasswordField(item.configKey)"
                    size="large"
                >
                  <template #prefix>
                    <el-icon class="text-gray-400"><component :is="getIcon(item.configKey)" /></el-icon>
                  </template>
                </el-input>
                <div v-if="item.configKey === 'dify_base_url'" class="text-xs text-gray-400 mt-1.5 ml-1">
                  例如：https://api.dify.ai/v1
                </div>
              </el-form-item>
            </template>
          </div>
        </el-card>

        <!-- 分组：其他配置 -->
        <el-card v-if="otherConfigs.length > 0" shadow="never" class="border-0 rounded-xl">
          <template #header>
            <div class="flex items-center gap-2 border-l-4 pl-3 py-1 border-gray-400">
              <span class="font-bold text-gray-800 text-lg">通用配置</span>
            </div>
          </template>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            <template v-for="item in otherConfigs" :key="item.id">
              <el-form-item :label="item.description || item.configKey">
                <el-input v-model="item.configValue" size="large" />
              </el-form-item>
            </template>
          </div>
        </el-card>
      </div>

      <!-- 底部固定操作栏 -->
      <div class="shrink-0 pt-4 flex justify-end sticky bottom-0 z-10 bg-gray-50">
        <div class="bg-white px-8 py-3 rounded-full shadow-lg border border-gray-200 flex gap-4">
          <el-button @click="fetchConfig" :icon="Refresh" round>重置更改</el-button>
          <el-button type="primary" color="#6366f1" @click="handleSubmit" :loading="submitLoading" :icon="Check" round>
            保存全部配置
          </el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Link, Key, Collection, Refresh, Check, Setting } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ConfigItem {
  id: number
  configKey: string
  configValue: string
  description: string
}

const loading = ref(true)
const submitLoading = ref(false)
const configList = ref<ConfigItem[]>([])
const formModel = reactive({})

const difyConfigs = computed(() => configList.value.filter(item => item.configKey.startsWith('dify_')))
const otherConfigs = computed(() => configList.value.filter(item => !item.configKey.startsWith('dify_')))

const isPasswordField = (key: string) => key.includes('key') || key.includes('secret') || key.includes('password')
const isLongField = (key: string) => key === 'dify_base_url'
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
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    await request.put('/admin/config', configList.value)
    ElMessage.success('配置保存成功')
    await fetchConfig()
  } catch (error) {
    // error
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
  transition: all 0.2s;
  background-color: #f9fafb;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c7d2fe inset;
  background-color: #fff;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #6366f1 inset !important;
  background-color: #fff;
}
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
</style>