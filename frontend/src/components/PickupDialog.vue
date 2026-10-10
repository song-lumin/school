<template>
  <el-dialog :model-value="visible" title="现场确认领取" width="560px" @update:model-value="$emit('update:visible', $event)" @closed="reset">
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <p>请确认您已在投放点找到本人失物。在认领单上填写姓名和日期，将<b>认领单与失物合影</b>拍照上传，并由领取人<b>手写签名</b>确认。</p>
    </el-alert>

    <el-form label-width="90px">
      <el-form-item required label="认领人姓名">
        <el-input v-model="claimerName" placeholder="请输入认领人姓名（用于留档）" maxlength="20" />
      </el-form-item>
      <el-form-item required label="现场合影">
        <el-upload
          v-model:file-list="fileList"
          :auto-upload="false"
          :limit="1"
          accept="image/jpeg,image/png,image/gif"
          list-type="picture-card"
          :on-change="handleFileChange"
          :on-remove="() => (fileList = [])"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item required label="领取人签名">
        <div class="signature-wrapper">
          <canvas
            ref="signatureCanvas"
            width="440"
            height="160"
            class="signature-canvas"
            @mousedown="startDraw"
            @mousemove="onDrawing"
            @mouseup="endDraw"
            @mouseleave="endDraw"
            @touchstart.prevent="startDrawTouch"
            @touchmove.prevent="onDrawing"
            @touchend="endDraw"
          ></canvas>
          <el-button link type="danger" size="small" @click="initCanvas" style="margin-top: 4px">清空重签</el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认是我的，完成领取</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { claimApi, uploadApi } from '@/api'

const props = defineProps<{
  visible: boolean
  claimId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const submitting = ref(false)
const fileList = ref<UploadUserFile[]>([])
const claimerName = ref('')
const signatureCanvas = ref<HTMLCanvasElement | null>(null)
const isDrawing = ref(false)
let hasSignature = false

const reset = () => {
  fileList.value = []
  claimerName.value = ''
  hasSignature = false
}

const initCanvas = () => {
  const canvas = signatureCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.strokeStyle = '#303133'
  ctx.lineWidth = 2
  ctx.lineJoin = 'round'
  ctx.lineCap = 'round'
  hasSignature = false
}

const getPos = (e: MouseEvent | TouchEvent) => {
  const canvas = signatureCanvas.value!
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height
  if ('touches' in e) {
    return { x: (e.touches[0].clientX - rect.left) * scaleX, y: (e.touches[0].clientY - rect.top) * scaleY }
  }
  return { x: (e.clientX - rect.left) * scaleX, y: (e.clientY - rect.top) * scaleY }
}

const startStroke = (e: MouseEvent | TouchEvent) => {
  const ctx = signatureCanvas.value!.getContext('2d')!
  const pos = getPos(e)
  isDrawing.value = true
  ctx.beginPath()
  ctx.moveTo(pos.x, pos.y)
}

const startDraw = (e: MouseEvent) => startStroke(e)
const startDrawTouch = (e: TouchEvent) => startStroke(e)

const onDrawing = (e: MouseEvent | TouchEvent) => {
  if (!isDrawing.value) return
  const ctx = signatureCanvas.value!.getContext('2d')!
  const pos = getPos(e)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  hasSignature = true
}

const endDraw = () => {
  isDrawing.value = false
}

const handleFileChange = (file: UploadFile) => {
  const isValidType = ['image/jpeg', 'image/png', 'image/gif'].includes(file.raw?.type || '')
  if (!isValidType) {
    ElMessage.error('仅支持 jpg/png/gif 格式')
    fileList.value = []
    return
  }
  if ((file.raw?.size || 0) > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    fileList.value = []
  }
}

const canvasToBlob = (): Promise<Blob | null> => {
  const canvas = signatureCanvas.value!
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob), 'image/png'))
}

const handleSubmit = async () => {
  if (!props.claimId) return
  if (!claimerName.value.trim()) {
    ElMessage.warning('请填写认领人姓名')
    return
  }
  if (fileList.value.length === 0 || !fileList.value[0].raw) {
    ElMessage.warning('请上传现场合影照片')
    return
  }
  if (!hasSignature) {
    ElMessage.warning('请领取人在签名板上签名')
    return
  }

  submitting.value = true
  try {
    const photoRes = await uploadApi.uploadImage(fileList.value[0].raw)
    const sigBlob = await canvasToBlob()
    if (!sigBlob) {
      ElMessage.error('签名生成失败，请重试')
      return
    }
    const sigFile = new File([sigBlob], `signature-${props.claimId}.png`, { type: 'image/png' })
    const sigRes = await uploadApi.uploadImage(sigFile)
    await claimApi.pickup(props.claimId, { pickupPhoto: photoRes.data, pickupSignature: sigRes.data })
    ElMessage.success('取件确认完成，积分已发放')
    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('取件确认失败:', error)
  } finally {
    submitting.value = false
  }
}

defineExpose({
  init: () => nextTick(initCanvas)
})
</script>

<style scoped>
.signature-wrapper {
  width: 100%;
}

.signature-canvas {
  width: 100%;
  max-width: 440px;
  height: 160px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #fff;
  cursor: crosshair;
  touch-action: none;
}
</style>
