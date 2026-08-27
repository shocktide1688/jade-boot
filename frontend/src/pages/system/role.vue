<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref<any[]>([])
const showDialog = ref(false)
const editing = ref<any>({})

async function fetchData() {
  loading.value = true
  try {
    const res = await request({ url: '/api/v1/roles/all', method: 'GET' })
    list.value = res.data.data
  } finally { loading.value = false }
}

function openCreate() {
  editing.value = { roleCode: '', roleName: '', dataScope: 'ALL', status: 1, roleSort: 0 }
  showDialog.value = true
}

function openEdit(row: any) {
  editing.value = { ...row }
  showDialog.value = true
}

async function save() {
  if (editing.value.id) {
    await request({ url: `/api/v1/roles/${editing.value.id}`, method: 'PUT', data: editing.value })
  } else {
    await request({ url: '/api/v1/roles', method: 'POST', data: editing.value })
  }
  ElMessage.success('保存成功')
  showDialog.value = false
  fetchData()
}

async function remove(id: number) {
  await request({ url: `/api/v1/roles/${id}`, method: 'DELETE' })
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate" style="background: #00a86b; border-color: #00a86b;">新建角色</el-button>
    </div>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名" />
      <el-table-column prop="roleCode" label="角色代码" />
      <el-table-column prop="dataScope" label="数据范围" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row.id)" :disabled="row.roleCode === 'admin'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showDialog" :title="editing.id ? '编辑角色' : '新建角色'" width="500">
    <el-form :model="editing" label-width="100">
      <el-form-item label="角色名"><el-input v-model="editing.roleName" /></el-form-item>
      <el-form-item label="角色代码"><el-input v-model="editing.roleCode" :disabled="!!editing.id" /></el-form-item>
      <el-form-item label="数据范围">
        <el-select v-model="editing.dataScope">
          <el-option label="全部" value="ALL" />
          <el-option label="本部门及下级" value="DEPT_AND_CHILD" />
          <el-option label="本部门" value="DEPT" />
          <el-option label="仅本人" value="SELF" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="editing.status">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showDialog = false">取消</el-button>
      <el-button type="primary" @click="save" style="background: #00a86b; border-color: #00a86b;">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.toolbar { margin-bottom: 16px; }</style>
