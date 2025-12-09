<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover" class="entry-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="entry-list">
            <div class="entry-item" @click="$router.push('/teacher/paper-create')">
              <div class="icon-wrapper blue"><el-icon><DocumentAdd /></el-icon></div>
              <span>新建试卷</span>
            </div>
            <div class="entry-item" @click="$router.push('/teacher/exam-publish')">
              <div class="icon-wrapper green"><el-icon><Timer /></el-icon></div>
              <span>发布考试</span>
            </div>
            <div class="entry-item" @click="$router.push('/teacher/question-bank')">
              <div class="icon-wrapper orange"><el-icon><Edit /></el-icon></div>
              <span>录入试题</span>
            </div>
            <div class="entry-item" @click="$router.push('/teacher/review-console')">
              <div class="icon-wrapper purple"><el-icon><Finished /></el-icon></div>
              <span>批改试卷</span>
            </div>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="todo-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-tag type="danger" effect="dark">3</el-tag>
            </div>
          </template>
          <el-table :data="todoList" :show-header="false">
            <el-table-column width="40">
              <template #default><el-checkbox v-model="checked" /></template>
            </el-table-column>
            <el-table-column prop="content" />
            <el-table-column prop="time" width="100" align="right" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>本周数据概览</template>
          <div class="stat-item">
            <div class="label">发布考试</div>
            <el-progress :percentage="80" status="success" />
          </div>
          <div class="stat-item">
            <div class="label">已批改试卷</div>
            <el-progress :percentage="45" />
          </div>
          <div class="stat-item">
            <div class="label">题库新增</div>
            <el-progress :percentage="60" color="#e6a23c" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { DocumentAdd, Timer, Edit, Finished } from '@element-plus/icons-vue'

const checked = ref(false)
const todoList = ref([
  { content: '批改 "Java基础期中考试" 主观题', time: '今天' },
  { content: '审核 2023级 题库新增试题', time: '明天' },
  { content: '完善 "数据库原理" 课程大纲', time: '本周五' }
])
</script>

<style scoped lang="scss">
.entry-list {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
  
  .entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover { transform: translateY(-3px); }
    
    .icon-wrapper {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #fff;
      margin-bottom: 8px;
      
      &.blue { background: #409eff; }
      &.green { background: #67c23a; }
      &.orange { background: #e6a23c; }
      &.purple { background: #a561da; }
    }
    span { font-size: 14px; color: #606266; }
  }
}

.stat-item {
  margin-bottom: 20px;
  .label { font-size: 14px; color: #606266; margin-bottom: 6px; }
}
</style>
