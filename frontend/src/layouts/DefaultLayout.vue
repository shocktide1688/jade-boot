<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const userStore = useUserStore()
const appStore = useAppStore()
const router = useRouter()

const menus = [
  { path: '/dashboard', title: '首页', icon: 'House' },
  { path: '/system/user', title: '用户管理', icon: 'User' },
]

const activeMenu = computed(() => router.currentRoute.value.path)

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span v-if="!appStore.sidebarCollapsed">Jade Platform</span>
        <span v-else>玉</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#00a86b"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-button text @click="appStore.toggleSidebar">
          <el-icon><Expand v-if="appStore.sidebarCollapsed" /><Fold v-else /></el-icon>
        </el-button>
        <div class="user-area">
          <el-dropdown @command="handleLogout">
            <span class="user-name">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; transition: width 0.2s; }
.logo {
  height: 60px; line-height: 60px; color: #fff; text-align: center;
  font-size: 18px; font-weight: bold; letter-spacing: 2px;
}
.logo span { color: #00a86b; }
.aside :deep(.el-menu) { border-right: none; }
.header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e6e6e6; padding: 0 16px;
}
.user-area { display: flex; align-items: center; }
.user-name {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  color: #303133;
}
.main { background: #f5f7fa; padding: 16px; }
</style>
