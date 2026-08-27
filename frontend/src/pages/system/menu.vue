<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await request({ url: '/api/v1/menus/tree', method: 'GET' })
    list.value = res.data.data
  } finally { loading.value = false }
}

onMounted(fetchData)

const TYPE_LABELS: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
function typeLabel(t: string) { return TYPE_LABELS[t] || t }
</script>

<template>
  <el-card>
    <el-table v-loading="loading" :data="list" row-key="id" :tree-props="{ children: 'children' }" default-expand-all stripe>
      <el-table-column prop="menuName" label="菜单名" width="200" />
      <el-table-column prop="menuType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 'M' ? 'info' : row.menuType === 'C' ? 'success' : 'warning'" size="small">
            {{ typeLabel(row.menuType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由" />
      <el-table-column prop="component" label="组件" />
      <el-table-column prop="perms" label="权限标识" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '显示' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
