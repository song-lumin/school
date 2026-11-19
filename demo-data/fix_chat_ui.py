# -*- coding: utf-8 -*-
import io

def patch(path, reps, tag):
    s = io.open(path, encoding='utf-8').read()
    for old, new, name in reps:
        c = s.count(old)
        print(tag, name, 'match=', c)
        if c != 1:
            raise SystemExit('ABORT ' + tag + ' ' + name)
        s = s.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8', newline='').write(s)
    print(tag, 'saved')

base = r'D:\codex\codex-data\schoollose\frontend\src\views\\'

# ================= 用户端：我的申诉 =================
patch(base + 'DisputeView.vue', [
    ("""        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>""",
     """        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button link type="warning" @click="openChat(row)">沟通</el-button>
          </template>
        </el-table-column>""", 'action'),

    ("""      <el-pagination""",
     """      <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 站点与系统管理员" />

      <el-pagination""", 'dialog'),

    ("""import EvidenceUpload from '@/components/EvidenceUpload.vue'""",
     """import EvidenceUpload from '@/components/EvidenceUpload.vue'
import ChatDialog from '@/components/ChatDialog.vue'""", 'import'),

    ("""const disputeDialogVisible = ref(false)""",
     """const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const disputeDialogVisible = ref(false)""", 'script'),
], 'DisputeView')

# ================= 站点端 =================
patch(base + 'PointAdminView.vue', [
    ("""            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openDisputeCheck(row)">
                  {{ row.pointCheckedAt ? '补充核验' : '现场核验' }}
                </el-button>
                <el-button size="small" @click="openCameraApply(row)">调监控</el-button>
              </template>
            </el-table-column>""",
     """            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openDisputeCheck(row)">
                  {{ row.pointCheckedAt ? '补充核验' : '现场核验' }}
                </el-button>
                <el-button size="small" @click="openCameraApply(row)">调监控</el-button>
                <el-button size="small" type="warning" plain @click="openChat(row)">沟通</el-button>
              </template>
            </el-table-column>""", 'action'),

    ("""    <el-dialog v-model="cameraDialogVisible" title="申请调取监控" width="540px">""",
     """    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 申诉人与系统管理员" />

    <el-dialog v-model="cameraDialogVisible" title="申请调取监控" width="540px">""", 'dialog'),

    ("""const openDisputeCheck = (row: any) => {""",
     """const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const openDisputeCheck = (row: any) => {""", 'script'),
], 'PointAdminView')

# ================= 管理员端 =================
patch(base + 'AdminView.vue', [
    ("""                <el-button link type="primary" @click="openHandleDialog(row)">处理</el-button>
                <el-tag v-if="row.status === 3" size="small" type="primary" style="margin-left:6px">待定责</el-tag>""",
     """                <el-button link type="primary" @click="openHandleDialog(row)">处理</el-button>
                <el-button link type="warning" @click="openChat(row)">沟通</el-button>
                <el-tag v-if="row.status === 3" size="small" type="primary" style="margin-left:6px">待定责</el-tag>""", 'action'),

    ("""    <!-- 申诉处理对话框 -->""",
     """    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 申诉人与站点负责人" />

    <!-- 申诉处理对话框 -->""", 'dialog'),

    ("""const openHandleDialog = (row: DisputeVO) => {""",
     """const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const openHandleDialog = (row: DisputeVO) => {""", 'script'),
], 'AdminView')
print('all views patched')
