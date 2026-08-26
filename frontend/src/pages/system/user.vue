<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  status: number
  createdAt: string
}

const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await request({
      url: '/api/v1/users',
      method: 'GET',
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined },
    })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称"
        clearable
        style="width: 240px;"
        @keyup.enter="fetchData"
      />
      <el-button type="primary" style="background: #00a86b; border-color: #00a86b;" @click="fetchData">
        搜索
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end;"
      @current-change="fetchData"
      @size-change="fetchData"
    />
  </el-card>
</template>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
