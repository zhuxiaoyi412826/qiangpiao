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
        <div ref="stageRef" class="slider-stage"
             :style="{ width: slider.width + 'px', height: slider.height + 'px' }">
          <img v-if="slider.background" :src="slider.background" class="slider-bg" title="点击换一张"
               @click="loadSlider"/>
          <img v-if="slider.block" :src="slider.block" class="slider-block" :style="blockStyle"/>
          <div v-if="sliderLoading" class="slider-mask">加载中…</div>
          <div v-else-if="!slider.background" class="slider-mask">滑块加载失败，点击「刷新」重试</div>
        </div>
        <div class="slider-tip">
          <span class="muted">{{ slider.sliderId ? '拖动下方滑块，把拼图推到缺口位置' : '正在获取滑块…' }}</span>
          <el-button size="small" text type="primary" :loading="sliderLoading" @click="loadSlider">刷新</el-button>
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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
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
const stageRef = ref(null)
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
    removeDragListeners()
}

function onOpen() {
    if (tab.value === 'image') {
        loadImage()
    } else {
        loadSlider()
    }
}

/**
 * 切换页签时按需加载：
 * 之前只在弹窗打开那一刻按当前页签加载一次，切到「滑块验证」永远不会触发加载，
 * 表现为「滑块页空白、没有图也没法拖」——这是滑块验证失效的主因。
 */
watch(tab, v => {
    if (!visible.value) return
    if (v === 'image') {
        if (!captcha.value.image) loadImage()
    } else if (!slider.value.sliderId) {
        loadSlider()
    }
})

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

let moveHandler = null
let upHandler = null

/** 解绑拖动监听：拖动中途关闭弹窗 / 组件卸载时都要调用，避免监听残留 */
function removeDragListeners() {
    if (moveHandler) {
        document.removeEventListener('mousemove', moveHandler)
        document.removeEventListener('touchmove', moveHandler)
        moveHandler = null
    }
    if (upHandler) {
        document.removeEventListener('mouseup', upHandler)
        document.removeEventListener('touchend', upHandler)
        upHandler = null
    }
}

function startDrag(e) {
    if (!trackRef.value) return
    if (!slider.value.sliderId) {
        return ElMessage.warning('滑块尚未加载完成，请稍候或点「刷新」')
    }
    const rect = trackRef.value.getBoundingClientRect()
    moveTo(e, rect)
    // 每次按下都重新绑一份：先解绑旧的，避免反复拖动导致监听叠加
    removeDragListeners()
    moveHandler = ev => moveTo(ev, rect)
    upHandler = () => removeDragListeners()
    document.addEventListener('mousemove', moveHandler)
    document.addEventListener('touchmove', moveHandler)
    document.addEventListener('mouseup', upHandler)
    document.addEventListener('touchend', upHandler)
}

function moveTo(e, rect) {
    const clientX = e.touches && e.touches.length ? e.touches[0].clientX : e.clientX
    if (clientX == null) return
    // 按钮宽 44，按中心对齐拖动位置；上限按轨道实际宽度算，按钮不越界
    const x = clientX - rect.left - 22
    const max = Math.max(0, rect.width - 44)
    blockX.value = Math.max(0, Math.min(max, x))
}

/**
 * 显示坐标 → 原图坐标。
 * 图片若被 CSS 缩放，前端拖到的像素与后端缺口坐标就不是同一个尺度，
 * 不换算会出现「明明对齐了却一直验证失败」。
 */
function toOriginX(displayX) {
    const w = trackRef.value ? trackRef.value.getBoundingClientRect().width : 0
    if (!w || !slider.value.width) return Math.round(displayX)
    return Math.round(displayX * (slider.value.width / w))
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
    // 后端校验的是原图坐标，这里做一次尺度换算
    emit('ok', { sliderId: slider.value.sliderId, sliderX: toOriginX(blockX.value) })
    removeDragListeners()
    visible.value = false
}

onBeforeUnmount(removeDragListeners)

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

.slider-tip {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 6px 0 2px;
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
