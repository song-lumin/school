<template>
  <div class="evidence-upload">
    <div class="thumb-list">
      <div v-for="(url, idx) in modelValue" :key="`${url}-${idx}`" class="thumb">
        <el-image
          :src="url"
          :preview-src-list="modelValue"
          :initial-index="idx"
          fit="cover"
          class="thumb-img"
        />
        <button type="button" class="thumb-del" title="移除" @click="remove(idx)">×</button>
      </div>
      <label v-if="modelValue.length < max" class="thumb-add" :class="{ uploading }">
        <input type="file" accept="image/png,image/jpeg,image/gif" multiple :disabled="uploading" @change="onPick" />
        <span v-if="uploading">上传中…</span>
        <span v-else class="add-text">+<em>添加图片</em></span>
      </label>
    </div>
    <div class="tip">支持 jpg / png / gif，单张不超过 10MB，最多 {{ max }} 张，点击缩略图可放大</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadApi } from '@/api'

const props = withDefaults(defineProps<{ modelValue: string[]; max?: number }>(), { max: 6 })
const emit = defineEmits<{ (e: 'update:modelValue', value: string[]): void }>()

const uploading = ref(false)

const onPick = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return

  const remain = props.max - props.modelValue.length
  if (remain <= 0) {
    ElMessage.warning(`最多上传 ${props.max} 张图片`)
    return
  }
  if (files.length > remain) {
    ElMessage.warning(`最多上传 ${props.max} 张图片，本次只上传前 ${remain} 张`)
  }

  uploading.value = true
  const uploaded: string[] = []
  try {
    for (const file of files.slice(0, remain)) {
      if (!/^image\/(jpeg|png|gif)$/.test(file.type)) {
        ElMessage.warning(`${file.name} 不是支持的图片格式，已跳过`)
        continue
      }
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 10MB，已跳过`)
        continue
      }
      const res = await uploadApi.uploadImage(file)
      if (res.data) uploaded.push(res.data)
    }
    if (uploaded.length) {
      emit('update:modelValue', [...props.modelValue, ...uploaded])
      ElMessage.success(`已上传 ${uploaded.length} 张图片`)
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const remove = (idx: number) => {
  const next = props.modelValue.slice()
  next.splice(idx, 1)
  emit('update:modelValue', next)
}
</script>

<style scoped>
.evidence-upload {
  width: 100%;
}
.thumb-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.thumb {
  position: relative;
  width: 76px;
  height: 76px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
}
.thumb-img {
  width: 100%;
  height: 100%;
  display: block;
}
.thumb-del {
  position: absolute;
  top: 0;
  right: 0;
  width: 22px;
  height: 22px;
  line-height: 18px;
  border: none;
  border-radius: 0 0 0 6px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  padding: 0;
}
.thumb-del:hover {
  background: #c45656;
}
.thumb-add {
  width: 76px;
  height: 76px;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  cursor: pointer;
  background: #fafafa;
}
.thumb-add:hover {
  border-color: #2f7a63;
  color: #2f7a63;
}
.thumb-add.uploading {
  cursor: progress;
  font-size: 12px;
}
.thumb-add input[type='file'] {
  display: none;
}
.add-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 18px;
  line-height: 1.1;
}
.add-text em {
  font-size: 12px;
  font-style: normal;
  margin-top: 3px;
}
.tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
