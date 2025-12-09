<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- 页面标题和操作按钮 -->
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-bold">系统设置</h2>
        <el-button type="primary" @click="handleSubmit" :loading="saving">保存设置</el-button>
      </div>

      <!-- 系统配置表单 -->
      <el-form ref="formRef" :model="formData" label-width="200px" :rules="rules" class="w-full">
        <el-form-item v-for="config in configList" :key="config.id" :label="config.description" :prop="'config_' + config.id">
          <el-input
            v-model="formData['config_' + config.id]"
            :placeholder="'请输入' + config.description"
            :disabled="isSensitiveConfig(config.configKey)"
            :type="isPasswordConfig(config.configKey) ? 'password' : 'text'"
          >
            <template #append>
              <el-button v-if="isPasswordConfig(config.configKey)" @click="handleShowPassword(config.id)">
                <el-icon>{{ showPasswordMap['config_' + config.id] ? 'View' : 'Hide' }}</el-icon>
              </el-button>
            </template>
          </el-input>
          <el-input
            v-if="isSensitiveConfig(config.configKey)"
            :value="'******'"
            disabled
            class="mt-2"
            placeholder="敏感配置项，如需修改请联系管理员"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

// 数据定义
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const configList = ref<any[]>([])
const showPasswordMap = ref<Record<string, boolean>>({})

// 表单数据
const formData = reactive<any>({})

// 表单验证规则
const rules = reactive<any>({})

// 初始化
onMounted(() => {
  fetchConfig()
})

// 获取系统配置
const fetchConfig = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/admin/config')
    configList.value = res
    
    // 初始化表单数据和验证规则
    configList.value.forEach(config => {
      const fieldName = 'config_' + config.id
      formData[fieldName] = config.configValue
      rules[fieldName] = [{ required: true, message: `请输入${config.description}`, trigger: 'blur' }]
    })
  } catch (error) {
    ElMessage.error('获取系统配置失败')
  } finally {
    loading.value = false
  }
}

// 判断是否为敏感配置项
const isSensitiveConfig = (configKey: string) => {
  const lowerKey = configKey.toLowerCase()
  return lowerKey.includes('password') || lowerKey.includes('key') || 
         lowerKey.includes('secret') || lowerKey.includes('token')
}

// 判断是否为密码类型配置项
const isPasswordConfig = (configKey: string) => {
  return configKey.toLowerCase().includes('password')
}

// 切换密码显示状态
const handleShowPassword = (configId: number) => {
  const fieldName = 'config_' + configId
  showPasswordMap.value[fieldName] = !showPasswordMap.value[fieldName]
}

// 提交保存配置
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      saving.value = true
      try {
        // 构建配置更新列表
        const updateList = configList.value.map(config => {
          const fieldName = 'config_' + config.id
          return {
            id: config.id,
            configKey: config.configKey,
            configValue: formData[fieldName],
            description: config.description
          }
        })

        // 调用更新接口
        await request.put('/api/admin/config', updateList)
        
        ElMessage.success('系统设置保存成功')
      } catch (error) {
        // error handled by interceptor
      } finally {
        saving.value = false
      }
    }
  })
}
</script>
