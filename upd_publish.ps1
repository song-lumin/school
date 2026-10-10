$f = "D:\codex\codex-data\schoollose\frontend\src\views\ItemPublishView.vue"
$lines = [System.Collections.Generic.List[string]](Get-Content $f -Encoding UTF8)

# Find the closing </el-form-item> after claimQuestion
$claimIdx = -1
for ($i=0; $i -lt $lines.Count; $i++) {
  if ($lines[$i] -match 'label="防伪问题"') { $claimIdx = $i; break }
}
# Find closing </el-form-item>
$closeIdx = -1
for ($i=$claimIdx; $i -lt $lines.Count; $i++) {
  if ($lines[$i] -match '^\s*</el-form-item>\s*$') { $closeIdx = $i; break }
}
Write-Output "claim at $claimIdx, close at $closeIdx"

$newBlock = @(
'        <el-form-item label="参考答案（选填，仅本人可见）">',
'          <el-input',
'            v-model="form.referenceAnswer"',
'            type="textarea"',
'            :rows="2"',
'            placeholder="写下你心中的正确答案作为本人备注，用于系统智能比对失主回答的置信度；失主看不到此字段，也不用于自动判题"',
'            maxlength="300"',
'            show-word-limit',
'          />',
'          <div class="field-tip standalone">开放性问题不设标准答案，参考答案只作为发布人备注；失主回答后系统会综合答案相似度、用户信用分、历史记录自动算 0-100 置信度分，按分数排序供你人工审核。</div>',
'        </el-form-item>'
)
$lines.InsertRange($closeIdx + 1, [string[]]$newBlock)

# Add referenceAnswer to form reactive
for ($i=0; $i -lt $lines.Count; $i++) {
  if ($lines[$i] -match "claimQuestion: '',") {
    $lines.Insert($i + 1, "  referenceAnswer: '',")
    break
  }
}

# Add to submit payload
for ($i=0; $i -lt $lines.Count; $i++) {
  if ($lines[$i] -match "claimQuestion: form.claimQuestion,") {
    $lines.Insert($i + 1, "      referenceAnswer: form.referenceAnswer || undefined,")
    break
  }
}

[System.IO.File]::WriteAllLines($f, $lines, (New-Object System.Text.UTF8Encoding $false))
Write-Output "done"
