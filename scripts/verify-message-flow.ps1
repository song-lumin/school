$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8083/api'

function Login([string]$u) {
  $r = Invoke-RestMethod -Uri "$api/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (@{ username = $u; password = '123456' } | ConvertTo-Json)
  if ($r.code -ne 200) { throw "login $u failed: $($r.message)" }
  return $r.data.token
}

function Api([string]$method, [string]$path, [string]$token, $body) {
  $h = @{ Authorization = "Bearer $token" }
  $req = @{ Uri = "$api$path"; Method = $method; Headers = $h }
  if ($null -ne $body) {
    $req.ContentType = 'application/json; charset=utf-8'
    $req.Body = ($body | ConvertTo-Json -Depth 6)
  }
  return Invoke-RestMethod @req
}

function Raw([string]$path, [string]$token) {
  $h = @{ Authorization = "Bearer $token" }
  $resp = Invoke-WebRequest -Uri "$api$path" -Headers $h -UseBasicParsing
  $txt = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
  return ($txt | ConvertFrom-Json)
}

function ShowMessages([string]$label, [string]$token) {
  $r = Raw '/messages?page=1&size=30' $token
  Write-Host ("--- " + $label + " total=" + $r.data.total)
  foreach ($m in $r.data.records) {
    $first = $m.content.Substring(0, [Math]::Min(90, $m.content.Length)).Replace("`r", ' ').Replace("`n", ' ')
    Write-Host ("    [" + $m.bizType + "] " + $m.title + " | " + $first)
  }
  $u = Raw '/messages/unread-count' $token
  Write-Host ("    unread=" + $u.data.count)
}

$tAdmin = Login 'admin'
$tP1 = Login 'pointadmin1'
$tU4 = Login 'user4'
$tU5 = Login 'user5'

Write-Host '=== 1. user4 publishes a temp item ==='
$body = @{
  title = 'MSG-TEST item'
  category = 'other'
  description = 'message flow verification'
  foundLocation = 'demo hall'
  dropPointId = 1
  foundTime = '2026-11-19T09:00:00'
  claimQuestion = 'color?'
  referenceAnswer = 'blue'
  perishable = 0
}
$r = Api 'Post' '/found-items' $tU4 $body
if ($r.code -ne 200) { throw "publish failed: $($r.message)" }
$itemId = $r.data.id
Write-Host ("itemId=" + $itemId + " status=" + $r.data.itemStatus)

Write-Host '=== 2. user4 places the item, point admin checks in (expect founder +1 message) ==='
$r = Api 'Put' "/found-items/$itemId/mark-placed" $tU4 $null
Write-Host ("mark-placed: " + $r.code)
$r = Api 'Put' '/drop-points/1/check' $tP1 @{ itemId = $itemId; checkNote = 'checked' }
Write-Host ("check: " + $r.code + " " + $r.message)
Write-Host ("status=" + (Api 'Get' "/found-items/$itemId" $tU4 $null).data.itemStatus)

Write-Host '=== 3. user5 applies, approves, picks up (expect credit messages) ==='
$r = Api 'Post' '/claims' $tU5 @{ itemId = $itemId; answer = 'blue' }
$applyId = $r.data.id
Write-Host ("apply id=" + $applyId + " status=" + $r.data.applyStatus)
$r = Api 'Put' "/claims/$applyId/approve" $tP1 $null
Write-Host ("approve: " + $r.code)
$r = Api 'Put' "/claims/$applyId/pickup" $tU5 @{ pickupPhoto = 'demo.jpg'; pickupSignature = 'Wu Wang' }
Write-Host ("pickup: " + $r.code)

Write-Host '=== 4. user5 files ITEM_MISMATCH dispute, admin approves ==='
$r = Api 'Post' '/disputes' $tU5 @{ disputeType = 'ITEM_MISMATCH'; applyId = $applyId; itemId = $itemId; description = 'not mine after pickup' }
$disputeId = $r.data.id
Write-Host ("dispute id=" + $disputeId)
$r = Api 'Put' "/disputes/$disputeId/handle" $tAdmin @{ approved = $true; handlerNote = 'confirmed mismatch' }
Write-Host ("handle: " + $r.code)
Write-Host ("status=" + (Api 'Get' "/found-items/$itemId" $tU4 $null).data.itemStatus)

Write-Host '=== 5. point admin confirms return-in ==='
$r = Api 'Put' "/drop-points/1/items/$itemId/return-in" $tP1 $null
Write-Host ("return-in: " + $r.code)
Write-Host ("status=" + (Api 'Get' "/found-items/$itemId" $tU4 $null).data.itemStatus)

Write-Host '=== 6. messages ==='
ShowMessages 'founder user4' $tU4
ShowMessages 'claimer user5' $tU5

Write-Host ("VERIFY_ITEM_ID=" + $itemId + " VERIFY_DISPUTE_ID=" + $disputeId + " VERIFY_APPLY_ID=" + $applyId)
