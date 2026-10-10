<template>
  <div class="publish-container">
    <div class="publish-hero">
      <p class="hero-kicker">拾到物品 · 交给我们守护</p>
      <h1>发布招领</h1>
      <p class="hero-desc">填写清楚物品信息，失主就能更快找到它。发布后请将物品送到所选投放点。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="publish-form"
    >
      <section class="form-section">
        <h2 class="section-title">物品信息</h2>
        <el-form-item label="物品标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：黑色钱包一个" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="物品分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="颜色、品牌、外观特征等（注意不要写出防伪细节）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="拾取地点" prop="foundLocation" class="row-item">
            <el-input v-model="form.foundLocation" placeholder="你在哪里捡到的，例如：图书馆三楼自习区" maxlength="100" />
          </el-form-item>
          <el-form-item label="拾取时间" prop="foundTime" class="row-item">
            <el-date-picker
              v-model="form.foundTime"
              type="datetime"
              placeholder="选择拾取时间"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
        </div>
        <el-form-item label="投放点" prop="dropPointId">
          <el-select v-model="form.dropPointId" placeholder="你把物品放到哪个站点" style="width: 100%">
            <el-option
              v-for="dp in dropPoints"
              :key="dp.id"
              :label="`${dp.name}（${dp.location}）`"
              :value="dp.id"
            />
          </el-select>
          <div class="field-tip standalone">发布后招领立即公开，请尽快将物品送到该投放点，失主到站点认领。</div>
        </el-form-item>
        <el-form-item label="物品图片" prop="images">
          <div class="upload-area">
            <el-upload
              v-model:file-list="form.images"
              :auto-upload="false"
              list-type="picture-card"
              accept="image/jpeg,image/png,image/gif"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :limit="4"
              :on-exceed="handleExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">必传，最多 4 张，每张不超过 10MB，jpg/png/gif</div>
          </div>
        </el-form-item>
        <el-form-item label="易腐品">
          <el-switch v-model="form.perishable" :active-value="1" :inactive-value="0" />
          <span class="field-tip">易腐品会提示尽快投放</span>
        </el-form-item>
      </section>

      <section class="form-section">
        <h2 class="section-title">核验与发布</h2>
        <el-form-item label="防伪问题" prop="claimQuestion">
          <el-input
            v-model="form.claimQuestion"
            type="textarea"
            :rows="2"
            placeholder="例如：钱包里有什么卡？失主需答对才能认领"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="参考答案（选填，仅本人可见）">
          <el-input
            v-model="form.referenceAnswer"
            type="textarea"
            :rows="2"
            placeholder="写下你心中的正确答案作为本人备注，用于系统智能比对失主回答的置信度；失主看不到此字段，也不用于自动判题"
            maxlength="300"
            show-word-limit
          />
          <div class="field-tip standalone">开放性问题不设标准答案，参考答案只作为发布人备注；失主回答后系统会综合答案相似度、用户信用分、历史记录自动算 0-100 置信度分，按分数排序供你人工审核。</div>
        </el-form-item>
        <el-form-item label="代发">
          <el-switch v-model="isProxy" />
          <template v-if="isProxy">
            <el-input-number
              v-model="form.actualFounderId"
              :min="1"
              placeholder="实际拾得人用户ID"
              controls-position="right"
              style="margin-left: 12px; width: 200px"
            />
            <span class="field-tip">积分发给代发人（当前账号）</span>
          </template>
        </el-form-item>
        <div class="submit-bar">
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">发布招领</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
          <p class="submit-note">发布成功后，招领立即公开，请尽快将物品送到所选投放点。</p>
        </div>
      </section>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'
import { foundItemApi, uploadApi, dropPointApi } from '@/api'
import { ITEM_CATEGORIES } from '@/types'
import type { DropPoint } from '@/types'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const isProxy = ref(false)
const dropPoints = ref<DropPoint[]>([])

onMounted(async () => {
  try {
    const res = await dropPointApi.list()
    dropPoints.value = res.data.filter((dp) => dp.status === 1)
  } catch (error) {
    console.error('加载投放点失败:', error)
  }
})

const form = reactive({
  title: '',
  category: '',
  description: '',
  foundLocation: '',
  dropPointId: undefined as number | undefined,
  foundTime: '',
  images: [] as UploadUserFile[],
  perishable: 0,
  claimQuestion: '',
  referenceAnswer: '',
  actualFounderId: undefined as number | undefined
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入物品标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物品分类', trigger: 'change' }],
  foundLocation: [{ required: true, message: '请输入拾取地点', trigger: 'blur' }],
  dropPointId: [{ required: true, message: '请选择投放点', trigger: 'change' }],
  foundTime: [{ required: true, message: '请选择拾取时间', trigger: 'change' }],
  images: [
    {
      required: true,
      validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        if (form.images.length === 0) callback(new Error('请至少上传 1 张物品图片'))
        else callback()
      },
      trigger: 'change'
    }
  ],
  claimQuestion: [{ required: true, message: '请输入防伪问题', trigger: 'blur' }]
}

const handleExceed = () => {
  ElMessage.warning('最多上传 4 张图片')
}

const handleFileChange = (file: UploadFile) => {
  const isValidType = ['image/jpeg', 'image/png', 'image/gif'].includes(file.raw?.type || '')
  if (!isValidType) {
    ElMessage.error('仅支持 jpg/png/gif 格式')
    form.images = form.images.filter((f) => f.uid !== file.uid)
    return
  }
  if ((file.raw?.size || 0) > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    form.images = form.images.filter((f) => f.uid !== file.uid)
  }
  formRef.value?.validateField('images')
}

const handleFileRemove = () => {
  formRef.value?.validateField('images')
}

const uploadImages = async (): Promise<string[]> => {
  const urls: string[] = []
  for (const file of form.images) {
    if (file.raw) {
      const res = await uploadApi.uploadImage(file.raw)
      urls.push(res.data)
    }
  }
  return urls
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const images = await uploadImages()
    await foundItemApi.publish({
      title: form.title,
      category: form.category,
      description: form.description || undefined,
      foundLocation: form.foundLocation,
      dropPointId: form.dropPointId!,
      foundTime: form.foundTime,
      images: images.length > 0 ? images : undefined,
      claimQuestion: form.claimQuestion,
      referenceAnswer: form.referenceAnswer || undefined,
      perishable: form.perishable,
      actualFounderId: isProxy.value ? form.actualFounderId : undefined
    })
    ElMessage.success('发布成功，招领已公开，请尽快将物品送到投放点')
    router.push('/items')
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-container {
  max-width: 860px;
  margin: 0 auto;
}

.publish-hero { margin: 4px 0 22px; }
.hero-kicker { color: #39805e; font-size: 13px; font-weight: 550; }
.publish-hero h1 { margin: 8px 0 0; color: #26332f; font-size: 27px; font-weight: 650; }
.hero-desc { margin-top: 8px; color: #7d8a83; font-size: 13px; line-height: 1.7; }

.publish-form { display: flex; flex-direction: column; gap: 18px; }
.form-section { padding: 26px clamp(18px, 3vw, 30px) 28px; background: #fff; border: 1px solid #e2e9e4; }
.section-title { margin: 0 0 18px; padding-bottom: 12px; border-bottom: 1px solid #e6ece8; color: #26332f; font-size: 17px; font-weight: 650; }

.form-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.row-item { min-width: 0; }

.upload-area { width: 100%; }
.upload-area :deep(.el-upload--picture-card) { width: 96px; height: 96px; border-radius: 4px; }
.upload-area :deep(.el-upload-list--picture-card .el-upload-list__item) { width: 96px; height: 96px; border-radius: 4px; }
.upload-tip { color: #909399; font-size: 12px; margin-top: 4px; }

.field-tip { color: #909399; font-size: 12px; margin-left: 10px; }
.field-tip.standalone { display: block; margin: 4px 0 0; }

.submit-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding-top: 4px; }
.submit-note { flex-basis: 100%; margin: 0; color: #8a968f; font-size: 12px; }

@media (max-width: 640px) {
  .publish-hero h1 { font-size: 23px; }
  .form-section { padding: 20px 16px 22px; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
