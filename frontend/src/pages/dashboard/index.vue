<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const userStore = useUserStore()
const appStore = useAppStore()
</script>

<template>
  <div class="dashboard">
    <el-card class="welcome">
      <h2>欢迎回来，{{ userStore.userInfo?.nickname || userStore.username }} 👋</h2>
      <p>这是基于 Jade Platform 构建的管理后台骨架。</p>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card>
          <div class="stat-label">今日访问</div>
          <div class="stat-value">128</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-label">活跃用户</div>
          <div class="stat-value">42</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-label">订单数</div>
          <div class="stat-value">256</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-label">侧边栏</div>
          <el-switch v-model="appStore.sidebarCollapsed" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }
.welcome h2 { margin: 0 0 8px; color: #303133; }
.welcome p { margin: 0; color: #909399; }
.stat-row { margin-top: 8px; }
.stat-label { color: #909399; font-size: 14px; }
.stat-value { font-size: 28px; font-weight: 600; color: #00a86b; margin-top: 8px; }
</style>
