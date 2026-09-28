<template>
  <div class="publish-container">
    <el-card>
      <template #header>
        <h3>发布招领</h3>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        style="max-width: 640px"
      >
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
        <el-form-item label="拾取地点" prop="foundLocation">
          <el-input v-model="form.foundLocation" placeholder="例如：教学楼A栋201" maxlength="100" />
        </el-form-item>
        <el-form-item label="拾取时间" prop="foundTime">
          <el-date-picker
            v-model="form.foundTime"
            type="datetime"
            placeholder="选择拾取时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="物品图片">
          <div class="upload-area">
            <el-upload
              v-model:file-list="fileList"
              :auto-upload="false"
              list-type="picture-card"
              accept="image/jpeg,image/png,image/gif"
              :on-change="handleFileChange"
              :limit="4"
              :on-exceed="handleExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">最多 4 张，每张不超过 10MB，jpg/png/gif</div>
          </div>
        </el-form-item>
        <el-form-item label="易腐品">
          <el-switch v-model="form.perishable" :active-value="1" :inactive-value="0" />
          <span class="field-tip">易腐品会提示尽快投放</span>
        </el-form-item>
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
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">发布</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'
import { foundItemApi, uploadApi } from '@/api'
import { ITEM_CATEGORIES } from '@/types'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const isProxy = ref(false)
const fileList = ref<UploadUserFile[]>([])

const form = reactive({
  title: '',
  category: '',
  description: '',
  foundLocation: '',
  foundTime: '',
  perishable: 0,
  claimQuestion: '',
  actualFounderId: undefined as number | undefined
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入物品标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物品分类', trigger: 'change' }],
  foundLocation: [{ required: true, message: '请输入拾取地点', trigger: 'blur' }],
  foundTime: [{ required: true, message: '请选择拾取时间', trigger: 'change' }],
  claimQuestion: [{ required: true, message: '请输入防伪问题', trigger: 'blur' }]
}

const handleExceed = () => {
  ElMessage.warning('最多上传 4 张图片')
}

const handleFileChange = (file: UploadFile) => {
  const isValidType = ['image/jpeg', 'image/png', 'image/gif'].includes(file.raw?.type || '')
  if (!isValidType) {
    ElMessage.error('仅支持 jpg/png/gif 格式')
    fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
    return
  }
  if ((file.raw?.size || 0) > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
  }
}

const uploadImages = async (): Promise<string[]> => {
  const urls: string[] = []
  for (const file of fileList.value) {
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
      foundTime: form.foundTime,
      images: images.length > 0 ? images : undefined,
      claimQuestion: form.claimQuestion,
      perishable: form.perishable,
      actualFounderId: isProxy.value ? form.actualFounderId : undefined
    })
    ElMessage.success('发布成功，请尽快前往投放点交物')
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
  max-width: 900px;
  margin: 0 auto;
}

.upload-area {
  width: 100%;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.field-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}
</style>
