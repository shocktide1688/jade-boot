<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await request({ url: '/api/v1/depts/all', method: 'GET' })
    list.value = res.data.data
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="deptName" label="部门名" />
      <el-table-column prop="deptCode" label="部门代码" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
