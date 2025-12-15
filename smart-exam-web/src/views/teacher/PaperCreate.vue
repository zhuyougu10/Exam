<template>
  <div
      class="paper-create-container"
      style="height: calc(100vh - 110px); display: flex; flex-direction: column; background-color: #fff; border-radius: 8px; overflow: hidden; border: 1px solid #e5e7eb;"
  >
    <!-- 外层容器：固定高度，扣除 Layout 的 padding (20px*2) + Navbar (64px) = 104px (预留一点余量设为 110px) -->
    <!-- 使用 flex-col 确保头部和底部固定，中间自适应 -->
    <!-- 添加 overflow: hidden 防止最外层出现滚动条 -->
    <!-- 1. 顶部步骤条：紧凑设计 (红框区域优化) -->
    <!-- 减小 padding (12px 24px)，使高度变小 -->
    <div
        style="flex-shrink: 0; padding: 12px 24px; border-bottom: 1px solid #f3f4f6; background-color: #fff; z-index: 10;"
    >
      <div style="max-width: 800px; margin: 0 auto;">
        <el-steps :active="activeStep" finish-status="success" align-center class="compact-steps" simple>
          <el-step title="基本信息" icon="Edit" />
          <el-step :title="baseForm.mode === 'random' ? '策略配置' : '题目挑选'" :icon="baseForm.mode === 'random' ? 'MagicStick' : 'Mouse'" />
          <el-step title="预览确认" icon="View" />
        </el-steps>
      </div>
    </div>

    <!-- 2. 中间内容区：自适应高度，内部滚动 -->
    <!-- flex: 1 自动占满剩余空间，overflow: hidden 配合子元素的 overflow-y: auto 实现内部滚动 -->
    <div style="flex: 1; overflow: hidden; position: relative; background-color: #f9fafb;">

      <!-- === 步骤 1: 基本信息 === -->
      <div v-if="activeStep === 0" style="height: 100%; overflow-y: auto; display: flex; justify-content: center; padding-top: 32px;">
        <div style="width: 100%; max-width: 700px; padding: 0 20px;">
          <el-card shadow="never" style="border: 1px solid #e5e7eb; border-radius: 8px;">
            <template #header>
              <div style="display: flex; align-items: center; gap: 8px;">
                <div style="width: 4px; height: 18px; background-color: #4f46e5; border-radius: 2px;"></div>
                <span style="font-weight: 600; color: #1f2937; font-size: 16px;">试卷基本信息</span>
              </div>
            </template>
            <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="90px" size="large" style="margin-top: 10px;">
              <el-form-item label="试卷标题" prop="title">
                <el-input v-model="baseForm.title" placeholder="请输入试卷标题" maxlength="50" show-word-limit />
              </el-form-item>
              <el-form-item label="所属课程" prop="courseId">
                <el-select v-model="baseForm.courseId" placeholder="请选择课程" style="width: 100%;" filterable>
                  <el-option v-for="item in courseOptions" :key="item.id" :label="item.courseName" :value="item.id" />
                </el-select>
              </el-form-item>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="时长(分)" prop="duration">
                    <el-input-number v-model="baseForm.duration" :min="10" :step="10" :max="300" style="width: 100%;" controls-position="right">
                      <template #suffix>分钟</template>
                    </el-input-number>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="及格分" prop="passScore">
                    <el-input-number v-model="baseForm.passScore" :min="0" :max="100" style="width: 100%;" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="组卷模式" prop="mode" style="margin-top: 10px;">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; width: 100%;">
                  <div class="mode-card" :class="{ active: baseForm.mode === 'random' }" @click="baseForm.mode = 'random'">
                    <div class="icon-box" :class="baseForm.mode === 'random' ? 'active-icon' : 'normal-icon'">
                      <el-icon size="24"><MagicStick /></el-icon>
                    </div>
                    <div>
                      <div style="font-weight: bold; font-size: 14px; color: #374151;">智能组卷</div>
                      <div style="font-size: 12px; color: #6b7280; margin-top: 2px;">系统随机抽题</div>
                    </div>
                    <div v-if="baseForm.mode === 'random'" class="check-mark">
                      <el-icon><Check /></el-icon>
                    </div>
                  </div>
                  <div class="mode-card" :class="{ active: baseForm.mode === 'manual' }" @click="baseForm.mode = 'manual'">
                    <div class="icon-box" :class="baseForm.mode === 'manual' ? 'active-manual-icon' : 'normal-manual-icon'">
                      <el-icon size="24"><Mouse /></el-icon>
                    </div>
                    <div>
                      <div style="font-weight: bold; font-size: 14px; color: #374151;">手动组卷</div>
                      <div style="font-size: 12px; color: #6b7280; margin-top: 2px;">自由挑选题目</div>
                    </div>
                    <div v-if="baseForm.mode === 'manual'" class="check-mark manual">
                      <el-icon><Check /></el-icon>
                    </div>
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>

      <!-- === 步骤 2: 策略配置 (智能组卷) === -->
      <div v-else-if="activeStep === 1 && baseForm.mode === 'random'" style="height: 100%; overflow-y: auto; padding: 24px;">
        <div style="max-width: 900px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px;">
          <el-card shadow="never" style="border-radius: 8px; border: 1px solid #e5e7eb;">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <div style="width: 4px; height: 18px; background-color: #4f46e5; border-radius: 2px;"></div>
                  <span style="font-weight: bold; font-size: 16px;">策略配置</span>
                  <el-tag type="primary" effect="plain" round size="small" style="margin-left: 8px;">
                    当前总分: <span style="font-weight: bold;">{{ totalScore }}</span> 分
                  </el-tag>
                </div>
                <el-button type="primary" plain size="small" icon="Plus" @click="addRule">添加规则</el-button>
              </div>
            </template>

            <el-alert
                type="info"
                show-icon
                :closable="false"
                style="margin-bottom: 16px;"
                title="系统将根据下方配置的规则，从题库中随机抽取题目。请确保题库资源充足。"
            />

            <el-table :data="randomRules" border stripe style="width: 100%">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="题型" min-width="150">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small" style="width: 100%;">
                    <el-option label="单选题" :value="1" /><el-option label="多选题" :value="2" />
                    <el-option label="判断题" :value="3" /><el-option label="简答题" :value="4" />
                    <el-option label="填空题" :value="5" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="150">
                <template #default="{ row }">
                  <el-input-number v-model="row.count" :min="1" size="small" style="width: 100%;" />
                </template>
              </el-table-column>
              <el-table-column label="单分" width="150">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0.5" :step="0.5" size="small" style="width: 100%;" />
                </template>
              </el-table-column>
              <el-table-column label="小计" width="100" align="center">
                <template #default="{ row }">
                  <span style="font-weight: bold; color: #4f46e5;">{{ (row.count * row.score).toFixed(1) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="{ $index }">
                  <el-button type="danger" icon="Delete" circle text size="small" @click="removeRule($index)" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>

      <!-- === 步骤 2: 题目挑选 (手动组卷 - 修复溢出问题) === -->
      <!-- 关键修复：添加 min-height: 0 和 box-sizing: border-box -->
      <div v-else-if="activeStep === 1 && baseForm.mode === 'manual'"
           style="height: 100%; display: flex; gap: 16px; padding: 16px; overflow: hidden; box-sizing: border-box; min-height: 0;">

        <!-- 左侧：题库列表 (自适应，内部滚动) -->
        <!-- 关键修复：添加 min-width: 0 防止 flex 子项撑开 -->
        <div style="flex: 1; display: flex; flex-direction: column; background: #fff; border-radius: 8px; border: 1px solid #e5e7eb; min-width: 0; min-height: 0;">
          <div style="padding: 12px; border-bottom: 1px solid #f3f4f6; display: flex; gap: 12px; align-items: center; flex-shrink: 0;">
            <div style="font-weight: bold; color: #374151; white-space: nowrap; font-size: 15px;">待选题目</div>
            <el-select v-model="questionQuery.type" placeholder="题型" size="default" style="width: 100px;" clearable @change="handleQuestionSearch">
              <el-option label="单选" :value="1" /><el-option label="多选" :value="2" />
              <el-option label="判断" :value="3" /><el-option label="简答" :value="4" /><el-option label="填空" :value="5" />
            </el-select>
            <el-input v-model="questionQuery.content" placeholder="搜索..." size="default" prefix-icon="Search" clearable style="flex: 1;" @input="debouncedSearch" />
          </div>

          <div class="custom-scrollbar" style="flex: 1; overflow-y: auto; padding: 12px; background: #f9fafb;">
            <div v-for="q in questionList" :key="q.id" class="q-item-card">
              <div style="flex: 1; min-width: 0;">
                <div style="display: flex; gap: 8px; margin-bottom: 6px; align-items: center;">
                  <el-tag size="small" type="info" effect="light">{{ getTypeLabel(q.type) }}</el-tag>
                  <el-tag size="small" :type="q.difficulty === 1 ? 'success' : (q.difficulty === 2 ? 'warning' : 'danger')" effect="plain">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                </div>
                <div style="color: #374151; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="q.content">{{ q.content }}</div>
              </div>
              <el-button type="primary" icon="Plus" circle size="small" :disabled="isQuestionSelected(q.id)" @click="addQuestion(q)" />
            </div>
            <el-empty v-if="questionList.length === 0" :image-size="60" description="暂无数据" />
          </div>

          <div style="padding: 8px; border-top: 1px solid #f3f4f6; text-align: center; flex-shrink: 0;">
            <el-pagination small layout="prev, pager, next" :total="questionTotal" v-model:current-page="questionQuery.page" :page-size="questionQuery.size" @current-change="handleQuestionSearch" />
          </div>
        </div>

        <!-- 右侧：已选 (固定宽，内部滚动) -->
        <div style="width: 320px; display: flex; flex-direction: column; background: #fff; border-radius: 8px; border: 1px solid #e5e7eb; min-height: 0; min-width: 0; flex-shrink: 0;">
          <div style="padding: 12px; border-bottom: 1px solid #e0e7ff; background: #eef2ff; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;">
            <div style="font-weight: bold; color: #4338ca;">已选 ({{ manualQuestions.length }})</div>
            <span style="font-size: 12px; background: #fff; color: #4f46e5; padding: 2px 8px; border-radius: 4px; font-weight: bold;">总分: {{ totalScore }}</span>
          </div>
          <div class="custom-scrollbar" style="flex: 1; overflow-y: auto; padding: 12px; background: #f9fafb; position: relative;">
            <draggable v-model="manualQuestions" item-key="id" handle=".drag-handle" :animation="200" style="display: flex; flex-direction: column; gap: 8px;">
              <template #item="{ element, index }">
                <div style="background: #fff; padding: 8px; border-radius: 6px; border: 1px solid #e5e7eb; display: flex; gap: 8px; align-items: flex-start;">
                  <div class="drag-handle" style="cursor: move; color: #9ca3af; padding-top: 2px;"><el-icon><Rank /></el-icon></div>
                  <div style="flex: 1; min-width: 0;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                      <span style="font-size: 12px; color: #6b7280; background: #f3f4f6; padding: 0 4px; border-radius: 2px;">{{ index + 1 }}. {{ getTypeLabel(element.type) }}</span>
                    </div>
                    <div style="font-size: 12px; color: #4b5563; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 8px;">{{ element.content }}</div>
                    <div style="display: flex; align-items: center; justify-content: space-between;">
                      <el-input-number v-model="element.score" size="small" :min="0.5" :step="0.5" controls-position="right" style="width: 70px;" />
                      <el-button type="danger" icon="Close" link size="small" @click="removeQuestion(index)" />
                    </div>
                  </div>
                </div>
              </template>
            </draggable>
            <div v-if="manualQuestions.length === 0" style="height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #9ca3af;">
              <el-icon size="32" style="margin-bottom: 8px; color: #e5e7eb;"><DocumentAdd /></el-icon>
              <span style="font-size: 13px;">请从左侧添加题目</span>
            </div>
          </div>
        </div>
      </div>

      <!-- === 步骤 3: 预览确认 (现代化机试预览) === -->
      <div v-else-if="activeStep === 2" style="height: 100%; overflow-y: auto; padding: 24px 32px; background-color: #f3f4f6; display: flex; justify-content: center;">
        <div style="width: 100%; max-width: 900px; display: flex; flex-direction: column; gap: 20px;">

          <!-- 试卷头部卡片 -->
          <div style="background-color: #fff; border-radius: 8px; padding: 24px; border: 1px solid #e5e7eb; text-align: center;">
            <h2 style="font-size: 22px; font-weight: bold; color: #111827; margin-bottom: 16px;">{{ baseForm.title || '未命名试卷' }}</h2>

            <div style="display: flex; justify-content: center; gap: 16px; flex-wrap: wrap;">
              <div style="background: #f3f4f6; padding: 4px 12px; border-radius: 16px; font-size: 13px; color: #4b5563;">
                课程：{{ getCourseName(baseForm.courseId) }}
              </div>
              <div style="background: #fff7ed; color: #c2410c; padding: 4px 12px; border-radius: 16px; font-size: 13px;">
                时长：{{ baseForm.duration }} 分钟
              </div>
              <div style="background: #f0fdf4; color: #15803d; padding: 4px 12px; border-radius: 16px; font-size: 13px;">
                总分：{{ totalScore }} 分
              </div>
            </div>
          </div>

          <!-- 智能组卷策略展示 -->
          <div v-if="baseForm.mode === 'random'" style="background-color: #fff; border-radius: 8px; padding: 24px; border: 1px solid #e5e7eb;">
            <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6;">
              <div style="width: 36px; height: 36px; background: #eff6ff; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
                <el-icon size="18" color="#3b82f6"><MagicStick /></el-icon>
              </div>
              <div>
                <div style="font-weight: bold; font-size: 15px;">智能随机组卷</div>
                <div style="font-size: 12px; color: #6b7280;">系统将根据以下策略配置实时生成试卷</div>
              </div>
            </div>

            <el-table :data="randomRules" :border="false" style="width: 100%" size="small">
              <el-table-column prop="type" label="题型" align="left">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain" type="info">{{ getTypeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="数量" align="center">
                <template #default="{ row }">{{ row.count }} 题</template>
              </el-table-column>
              <el-table-column prop="score" label="单题分值" align="center">
                <template #default="{ row }">{{ row.score }} 分</template>
              </el-table-column>
              <el-table-column label="总计" align="right">
                <template #default="{ row }">
                  <span style="font-weight: bold; color: #4f46e5;">{{ (row.count * row.score).toFixed(1) }} 分</span>
                </template>
              </el-table-column>
            </el-table>
            <div style="background-color: #f9fafb; padding: 12px 24px; text-align: right; border-top: 1px solid #e5e7eb; margin-top: 0;">
              <span style="color: #6b7280; margin-right: 12px; font-size: 13px;">全卷合计:</span>
              <span style="font-size: 16px; font-weight: bold; color: #4f46e5;">{{ totalScore }} 分</span>
            </div>
          </div>

          <!-- 手动组卷模式预览 (现代化列表) -->
          <div v-else style="display: flex; flex-direction: column; gap: 16px;">
            <template v-for="type in [1, 2, 3, 4, 5]" :key="type">
              <div v-if="groupedQuestions[type] && groupedQuestions[type].length > 0"
                   style="background-color: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e5e7eb;">

                <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding-left: 10px; border-left: 3px solid #4f46e5;">
                  <div style="font-size: 15px; font-weight: bold; color: #1f2937;">{{ getGroupTitle(type) }}</div>
                  <div style="font-size: 12px; color: #6b7280;">
                    共 {{ groupedQuestions[type].length }} 题 · {{ getGroupScore(groupedQuestions[type]) }} 分
                  </div>
                </div>

                <div style="display: flex; flex-direction: column; gap: 12px;">
                  <div v-for="(q, idx) in groupedQuestions[type]" :key="q.id"
                       style="background-color: #f9fafb; border-radius: 6px; padding: 12px;">
                    <div style="display: flex; gap: 8px; font-size: 14px; color: #374151; line-height: 1.5;">
                      <span style="font-weight: bold; color: #9ca3af; min-width: 20px;">{{ idx + 1 }}.</span>
                      <div style="flex: 1;">
                        <div style="margin-bottom: 8px;">
                          {{ q.content }}
                          <span style="font-size: 12px; color: #9ca3af; margin-left: 6px; font-weight: normal;">({{ q.score }}分)</span>
                        </div>

                        <!-- 选项展示 -->
                        <div v-if="[1, 2].includes(q.type) && q.options" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 8px; margin-top: 8px;">
                          <div v-for="(opt, oIdx) in parseOptions(q.options)" :key="oIdx"
                               style="display: flex; gap: 6px; align-items: flex-start; font-size: 13px; color: #4b5563;">
                            <div style="width: 20px; height: 20px; border-radius: 50%; border: 1px solid #d1d5db; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; color: #6b7280; flex-shrink: 0; background: #fff;">
                              {{ String.fromCharCode(65 + oIdx) }}
                            </div>
                            <span style="margin-top: 1px;">{{ opt }}</span>
                          </div>
                        </div>

                        <!-- 简答题/填空题 占位符 -->
                        <div v-if="q.type === 4 || q.type === 5" style="margin-top: 10px; padding: 8px; background-color: #fff; border: 1px dashed #d1d5db; border-radius: 4px; color: #d1d5db; font-size: 12px;">
                          考生答题区域
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- 底部留白 -->
          <div style="height: 20px;"></div>
        </div>
      </div>
    </div>

    <!-- 3. 底部操作栏：固定底部 -->
    <div style="flex-shrink: 0; background: #fff; border-top: 1px solid #e5e7eb; padding: 12px; display: flex; justify-content: center; gap: 16px; z-index: 20;">
      <el-button v-if="activeStep > 0" @click="prevStep" :icon="ArrowLeft" round>上一步</el-button>
      <el-button v-if="activeStep < 2" type="primary" @click="nextStep" round>下一步 <el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
      <el-button v-if="activeStep === 2" type="success" :loading="submitting" @click="submitPaper" :icon="Check" round>确认创建</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  MagicStick, Mouse, Collection, List, Plus, Close,
  ArrowLeft, ArrowRight, Check, Rank, Search, Edit, View, DocumentAdd
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import draggable from 'vuedraggable'
import { useDebounceFn } from '@vueuse/core'

const router = useRouter()

// State
const activeStep = ref(0)
const submitting = ref(false)
const courseOptions = ref<any[]>([])
const baseFormRef = ref()

const baseForm = reactive({
  title: '',
  courseId: undefined as number | undefined,
  duration: 90,
  passScore: 60,
  mode: 'random'
})

const baseRules = {
  title: [{ required: true, message: '请输入试卷标题', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入及格分数', trigger: 'blur' }]
}

const randomRules = ref([{ type: 1, count: 5, score: 2 }, { type: 2, count: 2, score: 5 }])
const manualQuestions = ref<any[]>([])
const questionList = ref<any[]>([])
const questionTotal = ref(0)
const questionQuery = reactive({ page: 1, size: 20, content: '', type: undefined as number | undefined })

// Computed
const totalScore = computed(() => {
  if (baseForm.mode === 'random') {
    return randomRules.value.reduce((sum, item) => sum + (item.count * item.score), 0)
  } else {
    return manualQuestions.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  }
})

const groupedQuestions = computed(() => {
  if (baseForm.mode !== 'manual') return {}
  const groups: Record<number, any[]> = {}
  manualQuestions.value.forEach(q => {
    if (!groups[q.type]) groups[q.type] = []
    groups[q.type].push(q)
  })
  return groups
})

onMounted(() => {
  fetchCourses()
})

// Methods
const fetchCourses = async () => {
  try {
    // 只获取当前用户已加入的课程
    const res: any = await request.get('/course/user/my-courses')
    courseOptions.value = res || []
  } catch (error) { console.error(error) }
}

const getCourseName = (id?: number) => {
  const c = courseOptions.value.find(i => i.id === id)
  return c ? c.courseName : '未选课程'
}

const nextStep = async () => {
  if (activeStep.value === 0) {
    if (!baseFormRef.value) return
    await baseFormRef.value.validate((valid: boolean) => {
      if (valid) {
        if (baseForm.mode === 'manual' && questionList.value.length === 0) {
          handleQuestionSearch()
        }
        activeStep.value++
      }
    })
  } else if (activeStep.value === 1) {
    if (totalScore.value <= 0) {
      ElMessage.warning('试卷总分必须大于 0')
      return
    }
    if (baseForm.passScore > totalScore.value) {
      ElMessage.error(`及格分数 (${baseForm.passScore}) 不能高于总分 (${totalScore.value})`)
      return
    }
    if (baseForm.mode === 'random' && randomRules.value.length === 0) {
      ElMessage.warning('请配置规则')
      return
    }
    if (baseForm.mode === 'manual' && manualQuestions.value.length === 0) {
      ElMessage.warning('请选择至少一道题目')
      return
    }
    activeStep.value++
  }
}

const prevStep = () => activeStep.value--
const addRule = () => randomRules.value.push({ type: 1, count: 1, score: 2 })
const removeRule = (index: number) => randomRules.value.splice(index, 1)

const handleQuestionSearch = async () => {
  if (!baseForm.courseId) return
  try {
    const res: any = await request.get('/question/list', { params: { ...questionQuery, courseId: baseForm.courseId } })
    questionList.value = res.records
    questionTotal.value = res.total
  } catch (error) { console.error(error) }
}

const debouncedSearch = useDebounceFn(() => { handleQuestionSearch() }, 300)
const isQuestionSelected = (id: number) => manualQuestions.value.some(q => q.id === id)
const addQuestion = (question: any) => { const q = { ...question, score: getDefaultScore(question.type) }; manualQuestions.value.push(q) }
const removeQuestion = (index: number) => manualQuestions.value.splice(index, 1)

const getDefaultScore = (type: number) => {
  switch (type) { case 1: return 2; case 2: return 4; case 3: return 2; case 4: return 10; case 5: return 2; default: return 2 }
}

const parseOptions = (jsonStr: string) => { try { return JSON.parse(jsonStr) || [] } catch (e) { return [] } }

const submitPaper = async () => {
  submitting.value = true
  try {
    const url = baseForm.mode === 'random' ? '/paper/random-create' : '/paper/manual-create'
    const payload: any = {
      courseId: baseForm.courseId, title: baseForm.title, duration: baseForm.duration, passScore: baseForm.passScore
    }
    if (baseForm.mode === 'random') payload.rules = randomRules.value
    else payload.questionList = manualQuestions.value.map(q => ({ questionId: q.id, score: q.score }))

    const res: any = await request.post(url, payload)
    ElMessage.success('试卷创建成功！')
    // 跳转到试卷列表并自动打开预览弹窗
    router.push({ path: '/teacher/paper-list', query: { previewPaperId: res.id } })
  } catch (error) { console.error(error) }
  finally { submitting.value = false }
}

const getTypeLabel = (type: number) => ({ 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题', 5: '填空题' } as any)[type] || '未知'
const getTypeTag = (type: number) => ({ 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' } as any)[type] || ''
const getDifficultyLabel = (diff: number) => ({ 1: '简单', 2: '中等', 3: '困难' } as any)[diff] || '未知'
const getGroupTitle = (type: number) => {
  const labels = ['一', '二', '三', '四', '五']
  return `${labels[type - 1] || '其他'}、${getTypeLabel(type)}`
}
const getGroupScore = (questions: any[]) => questions ? questions.reduce((sum, q) => sum + (Number(q.score) || 0), 0) : 0
</script>

<style scoped>
.mode-card {
  border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; cursor: pointer; display: flex; align-items: center; gap: 12px; position: relative; transition: all 0.2s;
}
.mode-card:hover { border-color: #a5b4fc; background: #fdfdff; }
.mode-card.active { border-color: #4f46e5; background: #eef2ff; }
.icon-box {
  width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center;
}
.normal-icon { background: #f3f4f6; color: #6b7280; }
.active-icon { background: #4f46e5; color: #fff; }
.normal-manual-icon { background: #f3f4f6; color: #6b7280; }
.active-manual-icon { background: #10b981; color: #fff; }
.check-mark { position: absolute; top: 0; right: 0; background: #4f46e5; color: #fff; border-bottom-left-radius: 8px; padding: 2px 6px; }
.check-mark.manual { background: #10b981; }

.q-item-card {
  background: #fff; padding: 12px; border-radius: 6px; border: 1px solid #e5e7eb; margin-bottom: 8px; display: flex; gap: 12px; align-items: flex-start;
}
.q-item-card:hover { border-color: #d1d5db; }

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
</style>