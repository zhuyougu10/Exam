<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <div class="max-w-4xl mx-auto">
      <!-- 标题 -->
      <div class="mb-6">
        <h2 class="text-2xl font-bold text-gray-800">系统设置</h2>
        <p class="text-gray-500 text-sm mt-1">管理系统全局参数及第三方服务配置。</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="py-10 text-center">
        <el-icon class="is-loading text-3xl text-indigo-500"><Loading /></el-icon>
        <p class="mt-2 text-gray-400">正在加载配置...</p>
      </div>

      <!-- 动态配置表单 -->
      <el-form
          v-else
          ref="formRef"
          :model="formModel"
          label-position="top"
          class="space-y-6"
      >
        <!-- 分组：Dify AI 配置 -->
        <el-card shadow="never" class="border-0 rounded-xl overflow-visible">
          <template #header>
            <div class="flex items-center gap-2 border-l-4 border-indigo-500 pl-3">
              <span class="font-bold text-gray-700">Dify AI 服务配置</span>
              <el-tag size="small" type="info">核心</el-tag>
            </div>
          </template>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <template v-for="item in difyConfigs" :key="item.id">
              <el-form-item :label="item.description || item.configKey" class="col-span-1" :class="{ 'md:col-span-2': isLongField(item.configKey) }">
                <el-input
                    v-model="item.configValue"
                    :placeholder="'请输入 ' + (item.description || item.configKey)"
                    :type="isPasswordField(item.configKey) ? 'password' : 'text'"
                    :show-password="isPasswordField(item.configKey)"
                >
                  <template #prefix>
                    <el-icon><component :is="getIcon(item.configKey)" /></el-icon>
                  </template>
                </el-input>
                <!-- 提示信息 -->
                <div v-if="item.configKey === 'dify_base_url'" class="text-xs text-gray-400 mt-1">
                  Dify API 的服务地址，例如：https://api.dify.ai/v1
                </div>
              </el-form-item>
            </template>
          </div>
        </el-card>

        <!-- 分组：其他配置 (如果有) -->
        <el-card v-if="otherConfigs.length > 0" shadow="never" class="border-0 rounded-xl overflow-visible">
          <template #header>
            <div class="flex items-center gap-2 border-l-4 border-gray-400 pl-3">
              <span class="font-bold text-gray-700">其他配置</span>
            </div>
          </template>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <template v-for="item in otherConfigs" :key="item.id">
              <el-form-item :label="item.description || item.configKey">
                <el-input v-model="item.configValue" />
              </el-form-item>
            </template>
          </div>
        </el-card>

        <!-- 操作按钮 -->
        <div class="flex justify-end pt-4 sticky bottom-6 z-10">
          <div class="bg-white/90 backdrop-blur px-6 py-3 rounded-full shadow-lg border border-gray-100 flex gap-4">
            <el-button @click="fetchConfig" :icon="Refresh">重置更改</el-button>
            <el-button type="primary" color="#6366f1" @click="handleSubmit" :loading="submitLoading" :icon="Check">
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
import { Loading, Link, Key, Collection, Refresh, Check, Setting } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 类型定义
interface ConfigItem {
  id: number
  configKey: string
  configValue: string
  description: string
  createTime?: string
  updateTime?: string
}

// 状态
const loading = ref(true)
const submitLoading = ref(false)
const configList = ref<ConfigItem[]>([])
// 表单模型绑定对象，虽然直接修改 configList 也可以，但为了el-form验证方便，这里用作占位
const formModel = reactive({})

// 计算属性：将配置分组
const difyConfigs = computed(() => configList.value.filter(item => item.configKey.startsWith('dify_')))
const otherConfigs = computed(() => configList.value.filter(item => !item.configKey.startsWith('dify_')))

// 辅助方法：判断是否为密码字段
const isPasswordField = (key: string) => {
  return key.includes('key') || key.includes('secret') || key.includes('password')
}

// 辅助方法：判断是否为长字段（占据整行）
const isLongField = (key: string) => {
  return key === 'dify_base_url'
}

// 辅助方法：获取图标
const getIcon = (key: string) => {
  if (key.includes('base_url')) return Link
  if (key.includes('dataset')) return Collection
  if (isPasswordField(key)) return Key
  return Setting
}

// 获取配置
const fetchConfig = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/config')
    // 后端返回的是 List<Config>
    configList.value = res
  } catch (error) {
    console.error('获取配置失败', error)
  } finally {
    loading.value = false
  }
}

// 提交保存
const handleSubmit = async () => {
  submitLoading.value = true
  try {
    // 直接提交修改后的列表，后端会根据 ID 和 Key 进行更新
    // 后端已有逻辑：如果值包含 '******' 则跳过更新，保护脱敏数据
    await request.put('/admin/config', configList.value)

    ElMessage.success('配置保存成功')
    // 重新获取以确保显示最新状态
    await fetchConfig()
  } catch (error) {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
/* 输入框美化 */
:deep(.el-input__wrapper) {
  padding: 8px 12px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
  transition: all 0.2s;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #6366f1 inset !important;
}
:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}
</style>