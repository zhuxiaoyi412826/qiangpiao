<template>
  <el-dialog v-model="visible" title="人机验证" width="440px" :close-on-click-modal="false" @open="onOpen">
    <el-tabs v-model="tab">
      <!-- 图形验证码 -->
      <el-tab-pane label="图形验证码" name="image">
        <div class="captcha-row">
          <el-input v-model="code" placeholder="请输入图中 4 位字符" maxlength="4" class="captcha-input"
                    @keyup.enter="confirm"/>
          <img v-if="captcha.image" :src="captcha.image" class="captcha-img" title="点击刷新" @click="loadImage"/>
          <el-button v-else :loading="imgLoading" @click="loadImage">获取验证码</el-button>
        </div>
        <p class="muted">不区分大小写，点击图片可刷新</p>
      </el-tab-pane>

      <!-- 滑块验证 -->
      <el-tab-pane label="滑块验证" name="slider">
        <div class="slider-stage" :style="{ width: slider.width + 'px', height: slider.height + 'px' }">
          <img v-if="slider.background" :src="slider.background" class="slider-bg"/>
          <img v-if="slider.block" :src="slider.block" class="slider-block" :style="blockStyle"/>
          <div v-if="sliderLoading" class="slider-mask">加载中…</div>
        </div>
        <div ref="trackRef" class="slider-track" :style="{ width: slider.width + 'px' }"
             @mousedown.prevent="startDrag" @touchstart.prevent="startDrag">
          <div class="slider-fill" :style="{ width: blockX + 'px' }"/>
          <div class="slider-btn" :style="{ left: blockX + 'px' }">⇄</div>
        </div>
        <p class="muted">拖动滑块补全拼图后点确认</p>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" @click="confirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCaptchaImage, getSlider } from '@/api/captcha'

const emit = defineEmits(['ok'])
const visible = ref(false)
const tab = ref('image')

// 图形验证码
const captcha = ref({ captchaId: '', image: '' })
const code = ref('')
const imgLoading = ref(false)

// 滑块
const slider = ref({ sliderId: '', background: '', block: '', blockY: 0, blockWidth: 52, blockHeight: 52, width: 320, height: 160 })
const blockX = ref(0)
const sliderLoading = ref(false)
const trackRef = ref(null)
const maxX = computed(() => Math.max(0, slider.value.width - slider.value.blockWidth))
const blockStyle = computed(() => ({
    left: blockX.value + 'px',
    top: slider.value.blockY + 'px',
    width: slider.value.blockWidth + 'px',
    height: slider.value.blockHeight + 'px'
}))

function open() {
    visible.value = true
}

function close() {
    visible.value = false
}

function onOpen() {
    if (tab.value === 'image') {
        loadImage()
    } else {
        loadSlider()
    }
}

async function loadImage() {
    imgLoading.value = true
    try {
        captcha.value = await getCaptchaImage()
        code.value = ''
    } catch (e) {
        ElMessage.error(e.message || '验证码加载失败')
    } finally {
        imgLoading.value = false
    }
}

async function loadSlider() {
    sliderLoading.value = true
    try {
        slider.value = await getSlider()
        blockX.value = 0
    } catch (e) {
        ElMessage.error(e.message || '滑块加载失败')
    } finally {
        sliderLoading.value = false
    }
}

function startDrag(e) {
    if (!trackRef.value) return
    const rect = trackRef.value.getBoundingClientRect()
    moveTo(e, rect)
    const onMove = ev => moveTo(ev, rect)
    const onUp = () => {
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
        document.removeEventListener('touchmove', onMove)
        document.removeEventListener('touchend', onUp)
    }
    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
    document.addEventListener('touchmove', onMove)
    document.addEventListener('touchend', onUp)
}

function moveTo(e, rect) {
    const clientX = e.touches && e.touches.length ? e.touches[0].clientX : e.clientX
    if (clientX == null) return
    // 按钮宽 44，按中心对齐拖动位置
    const x = clientX - rect.left - 22
    blockX.value = Math.max(0, Math.min(maxX.value, x))
}

function confirm() {
    if (tab.value === 'image') {
        if (!captcha.value.captchaId) {
            return ElMessage.warning('请先获取验证码')
        }
        if (!code.value || code.value.trim().length !== 4) {
            return ElMessage.warning('请输入 4 位验证码')
        }
        emit('ok', { captchaId: captcha.value.captchaId, captchaCode: code.value.trim() })
        visible.value = false
        return
    }
    if (!slider.value.sliderId) {
        return ElMessage.warning('请先获取滑块')
    }
    if (blockX.value <= 0) {
        return ElMessage.warning('请拖动滑块完成拼图')
    }
    emit('ok', { sliderId: slider.value.sliderId, sliderX: Math.round(blockX.value) })
    visible.value = false
}

onBeforeUnmount(() => {
    document.removeEventListener('mousemove', moveTo)
})

defineExpose({ open, close })
</script>

<style scoped>
.captcha-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.captcha-input {
    width: 170px;
}

.captcha-img {
    height: 40px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
}

.slider-stage {
    position: relative;
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: 10px;
    background: #f2f6fc;
}

.slider-bg {
    display: block;
    width: 100%;
    height: 100%;
}

.slider-block {
    position: absolute;
}

.slider-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
    font-size: 13px;
}

.slider-track {
    position: relative;
    height: 40px;
    border-radius: 20px;
    background: #e4e7ed;
    overflow: hidden;
    user-select: none;
}

.slider-fill {
    height: 100%;
    background: #c6e2ff;
}

.slider-btn {
    position: absolute;
    top: 0;
    width: 44px;
    height: 40px;
    line-height: 40px;
    text-align: center;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: grab;
    color: #409eff;
}

.slider-btn:active {
    cursor: grabbing;
}
</style>
