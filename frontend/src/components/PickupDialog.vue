<!--
  现场确认领取弹窗（PickupDialog）
  作用：当管理员/失主受理点把失物交还给认领人时，用这个弹窗完成"最后一步"的凭证采集。
  为什么需要它：单纯点"同意领取"无法证明物品真的交还了，所以这里强制要求三样东西——
    1. 认领人姓名（留档备查）
    2. 认领单与失物的现场合影（照片凭证）
    3. 领取人在签名板上的手写签名（法律/责任凭证）
  三者齐全后才调用后端 pickup 接口，认领单状态流转为"已领取"并发放诚信积分。
  被 ClaimListView（管理员处理认领）等页面复用。
-->
<template>
  <el-dialog :model-value="visible" title="现场确认领取" width="560px" @update:model-value="$emit('update:visible', $event)" @closed="reset">
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <p>请确认您已在投放点找到本人失物。在认领单上填写姓名和日期，将<b>认领单与失物合影</b>拍照上传，并由领取人<b>手写签名</b>确认。</p>
    </el-alert>

    <el-form label-width="90px">
      <el-form-item required label="认领人姓名">
        <el-input v-model="claimerName" placeholder="请输入认领人姓名（用于留档）" maxlength="20" />
      </el-form-item>
      <!-- 现场合影：不做自动上传（auto-upload=false），提交时才和签名一起上传 -->
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
      <!-- 手写签名板：canvas 同时监听鼠标事件和触摸事件，兼容 PC 与手机端签字 -->
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

// 组件入参：visible 控制弹窗显隐（配合 v-model:visible），claimId 是正在办理领取的认领单 ID
const props = defineProps<{
  visible: boolean
  claimId: number | null
}>()

// 对外事件：update:visible 关闭弹窗；success 通知父组件"领取已完成"，让父组件刷新列表
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const submitting = ref(false)          // 提交按钮 loading 态，防止重复提交
const fileList = ref<UploadUserFile[]>([])  // 现场合影（仅允许 1 张）
const claimerName = ref('')            // 认领人姓名
const signatureCanvas = ref<HTMLCanvasElement | null>(null)  // 签名板 canvas 的 DOM 引用
const isDrawing = ref(false)           // 是否正在书写签名（鼠标/手指按下的状态）
let hasSignature = false               // 是否已留下笔迹（用普通变量即可，无需响应式）

// 弹窗完全关闭后重置表单，下次打开是干净状态
const reset = () => {
  fileList.value = []
  claimerName.value = ''
  hasSignature = false
}

// 初始化签名板：铺一层白色底（否则导出的 PNG 是透明底），并设置画笔样式
const initCanvas = () => {
  const canvas = signatureCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)  // 白底填充整个画布
  ctx.strokeStyle = '#303133'   // 笔迹颜色：深灰
  ctx.lineWidth = 2             // 笔迹粗细
  ctx.lineJoin = 'round'        // 转角圆润，笔迹更接近真实书写
  ctx.lineCap = 'round'         // 端点圆润
  hasSignature = false
}

// 把鼠标/触摸事件的屏幕坐标换算成 canvas 内部坐标。
// 关键点：canvas 的 CSS 显示尺寸可能与实际像素尺寸(440x160)不一致，
// 必须按 scaleX/scaleY 缩放，否则手机端笔迹位置会偏移。
const getPos = (e: MouseEvent | TouchEvent) => {
  const canvas = signatureCanvas.value!
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height
  // TouchEvent 与 MouseEvent 的区分：'touches' in e 判断是否为触摸事件
  if ('touches' in e) {
    return { x: (e.touches[0].clientX - rect.left) * scaleX, y: (e.touches[0].clientY - rect.top) * scaleY }
  }
  return { x: (e.clientX - rect.left) * scaleX, y: (e.clientY - rect.top) * scaleY }
}

// 落笔：以当前点为起点开启一条新路径
const startStroke = (e: MouseEvent | TouchEvent) => {
  const ctx = signatureCanvas.value!.getContext('2d')!
  const pos = getPos(e)
  isDrawing.value = true
  ctx.beginPath()
  ctx.moveTo(pos.x, pos.y)
}

// 鼠标按下与触摸开始分别绑定（事件类型不同），内部走同一逻辑
const startDraw = (e: MouseEvent) => startStroke(e)
const startDrawTouch = (e: TouchEvent) => startStroke(e)

// 书写中：只有按住时才画线，每次移动从上一个点连到当前点并描边
const onDrawing = (e: MouseEvent | TouchEvent) => {
  if (!isDrawing.value) return
  const ctx = signatureCanvas.value!.getContext('2d')!
  const pos = getPos(e)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  hasSignature = true  // 一旦有笔迹就标记为"已签名"，用于提交前校验
}

// 抬笔/移出画布：结束本次书写
const endDraw = () => {
  isDrawing.value = false
}

// 现场合影的前端校验：类型只允许 jpg/png/gif，大小不超过 10MB，不合格立即清空选择
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

// 把签名 canvas 导出为 PNG 的二进制对象（Blob），用于后续上传
const canvasToBlob = (): Promise<Blob | null> => {
  const canvas = signatureCanvas.value!
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob), 'image/png'))
}

// 提交领取确认：校验三项凭证 → 上传合影 → 导出并上传签名 → 调用 pickup 接口
const handleSubmit = async () => {
  if (!props.claimId) return
  // 校验 1：认领人姓名必填
  if (!claimerName.value.trim()) {
    ElMessage.warning('请填写认领人姓名')
    return
  }
  // 校验 2：现场合影必传
  if (fileList.value.length === 0 || !fileList.value[0].raw) {
    ElMessage.warning('请上传现场合影照片')
    return
  }
  // 校验 3：签名板上必须留有笔迹
  if (!hasSignature) {
    ElMessage.warning('请领取人在签名板上签名')
    return
  }

  submitting.value = true
  try {
    // 第一步：上传现场合影，拿到图片 URL
    const photoRes = await uploadApi.uploadImage(fileList.value[0].raw)
    // 第二步：把签名 canvas 导出为 PNG 文件再上传（后端只存图片 URL，不存 canvas）
    const sigBlob = await canvasToBlob()
    if (!sigBlob) {
      ElMessage.error('签名生成失败，请重试')
      return
    }
    const sigFile = new File([sigBlob], `signature-${props.claimId}.png`, { type: 'image/png' })
    const sigRes = await uploadApi.uploadImage(sigFile)
    // 第三步：把两张凭证的 URL 提交给后端，认领单流转为"已领取"并触发积分发放
    await claimApi.pickup(props.claimId, { pickupPhoto: photoRes.data, pickupSignature: sigRes.data })
    ElMessage.success('取件确认完成，积分已发放')
    emit('update:visible', false)  // 关闭弹窗
    emit('success')                // 通知父组件刷新数据
  } catch (error) {
    // 请求失败的具体提示由 axios 拦截器统一弹出，这里只记录日志
    console.error('取件确认失败:', error)
  } finally {
    submitting.value = false
  }
}

// 暴露 init 方法给父组件：父组件打开弹窗后调用，等 DOM 渲染完成(nextTick)再初始化签名板
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
