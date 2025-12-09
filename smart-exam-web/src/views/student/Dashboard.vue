<template>
  <div class="dashboard-container">
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>早安，{{ userStore.userInfo?.realName }}同学！👋</h2>
          <p>距离期末考试还有 <span class="highlight">12</span> 天，继续加油！</p>
        </div>
        <img src="https://element-plus.org/images/element-plus-logo.svg" class="welcome-img" alt="welcome" />
      </div>
    </el-card>

    <el-row :gutter="20" class="data-row">
      <el-col :span="8">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>已完成考试</span>
              <el-tag type="success">Total</el-tag>
            </div>
          </template>
          <div class="card-body">
            <span class="number">12</span>
            <span class="label">场</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>平均分</span>
              <el-tag type="warning">Avg</el-tag>
            </div>
          </template>
          <div class="card-body">
            <span class="number">88.5</span>
            <span class="label">分</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>错题待处理</span>
              <el-tag type="danger">Action</el-tag>
            </div>
          </template>
          <div class="card-body">
            <span class="number">23</span>
            <span class="label">题</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="list-card">
      <template #header>
        <div class="card-header">
          <span>待参加考试</span>
          <el-button text type="primary">查看全部</el-button>
        </div>
      </template>
      <el-table :data="examList" style="width: 100%">
        <el-table-column prop="name" label="考试名称" />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="time" label="开始时间" width="180" />
        <el-table-column prop="duration" label="时长" width="100" />
        <el-table-column label="操作" width="120">
          <template #default>
            <el-button type="primary" size="small" round>去准备</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const examList = ref([
  {
    name: '2025年春季Java程序设计期中考试',
    subject: 'Java',
    time: '2025-05-20 14:00',
    duration: '120分钟'
  },
  {
    name: '计算机网络基础测验',
    subject: '网络',
    time: '2025-05-22 09:00',
    duration: '90分钟'
  }
])
</script>

<style scoped lang="scss">
.dashboard-container {
  .welcome-card {
    margin-bottom: 20px;
    background: linear-gradient(135deg, #e3f2fd 0%, #ffffff 100%);
    
    .welcome-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .welcome-text {
        h2 { margin: 0 0 10px; color: #303133; }
        p { margin: 0; color: #606266; }
        .highlight { color: #f56c6c; font-weight: bold; font-size: 1.2em; }
      }
      
      .welcome-img {
        height: 60px;
        opacity: 0.8;
      }
    }
  }

  .data-row {
    margin-bottom: 20px;
  }

  .data-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .card-body {
      display: flex;
      align-items: baseline;
      justify-content: center;
      padding: 10px 0;
      
      .number { font-size: 32px; font-weight: bold; color: #409eff; margin-right: 4px; }
      .label { font-size: 14px; color: #909399; }
    }
  }
}
</style>
