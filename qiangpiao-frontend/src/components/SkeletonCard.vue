<template>
  <div class="card-panel">
    <el-skeleton animated :rows="rows">
      <template #template>
        <el-skeleton-item v-if="showTitle" variant="h3" style="width: 40%; margin-bottom: 16px"/>
        <el-skeleton-item v-for="i in rows" :key="i" variant="text" style="margin-bottom: 10px"/>
        <div v-if="showTable" class="sk-table">
          <el-skeleton-item v-for="i in rows * 4" :key="'c' + i" variant="text" style="width: 100%"/>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
  showTitle: { type: Boolean, default: true },
  showTable: { type: Boolean, default: false }
})
</script>

<style scoped>
.sk-table {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
</style>
