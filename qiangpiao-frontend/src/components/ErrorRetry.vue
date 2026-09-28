<template>
  <div class="error-retry">
    <el-result icon="error" :title="title" :sub-title="message">
      <template #extra>
        <el-button type="primary" size="small" :loading="loading" @click="$emit('retry')">
          {{ buttonText }}
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '加载失败' },
  message: { type: String, default: '请检查网络后重试' },
  buttonText: { type: String, default: '重新加载' },
  loading: { type: Boolean, default: false }
})
defineEmits(['retry'])
</script>

<style scoped>
.error-retry {
  padding: 12px 0;
}

.error-retry :deep(.el-result__extra) {
  margin-top: 8px;
}
</style>
