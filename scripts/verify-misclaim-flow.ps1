$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8083/api'

function Login([string]$u, [string]$p) {
  $r = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body (@{ username = $u; password = $p } | ConvertTo-Json)
  if ($r.code -ne 200) { throw "login $u failed: $($r.message)" }
  return $r.data.token
}

function Api([string]$method, [string]$path, [string]$token, $body) {
  $h = @{ Authorization = "Bearer $token" }
  $req = @{ Uri = "$base$path"; Method = $method; Headers = $h }
  if ($null -ne $body) {
    $req.ContentType = 'application/json; charset=utf-8'
    $req.Body = ($body | ConvertTo-Json -Depth 6)
  }
  return Invoke-RestMethod @req
}

function ItemState([int]$id, [string]$token) {
  $r = Api 'Get' "/found-items/$id" $token $null
  return "item $id -> itemStatus=$($r.data.itemStatus) claimedAt=$($r.data.claimedAt)"
}

function UserCredit([int]$id, [string]$token) {
  $r = Api 'Get' "/admin/users?keyword=&page=1&size=200" $token $null
  $u = $r.data.records | Where-Object { $_.id -eq $id } | Select-Object -First 1
  return "user $id creditScore=$($u.creditScore)"
}

function CreditLogs([int]$id, [string]$token) {
  $r = Api 'Get' "/admin/users/$id/credit-logs" $token $null
  return ($r.data | Where-Object { $_.relatedItemId -eq 31 } | ForEach-Object { "$($_.operationType) $($_.changeAmount)" }) -join ' | '
}

$tAdmin = Login 'admin' '123456'
$tP1 = Login 'pointadmin1' '123456'
$tU1 = Login 'user1' '123456'
$tU2 = Login 'user2' '123456'

Write-Host '=== 0. initial state ==='
Write-Host (ItemState 31 $tU1)
Write-Host (UserCredit 5 $tAdmin)

Write-Host '=== 1. user2 picks up claim 23 (item 31) ==='
$r = Api 'Put' '/claims/23/pickup' $tU2 @{ pickupPhoto = 'demo-pickup.jpg'; pickupSignature = 'Li Si' }
Write-Host "pickup: $($r.code) $($r.message)"
Write-Host (ItemState 31 $tU1)
Write-Host (UserCredit 5 $tAdmin)
Write-Host ("creditlog user5 item31: " + (CreditLogs 5 $tAdmin))

Write-Host '=== 2. user2 files ITEM_MISMATCH dispute ==='
$r = Api 'Post' '/disputes' $tU2 @{ disputeType = 'ITEM_MISMATCH'; applyId = 23; itemId = 31; description = 'took it back and found it is not mine' }
Write-Host "apply dispute: $($r.code) $($r.message) id=$($r.data.id)"
$disputeId = $r.data.id

Write-Host '=== 3. admin approves (expect: credit rollback, item -> RETURNING(7)) ==='
$r = Api 'Put' "/disputes/$disputeId/handle" $tAdmin @{ approved = $true; handlerNote = 'confirmed mismatch, item to be returned' }
Write-Host "handle: $($r.code) $($r.message)"
Write-Host (ItemState 31 $tU1)
Write-Host (UserCredit 5 $tAdmin)
Write-Host ("creditlog user5 item31: " + (CreditLogs 5 $tAdmin))

Write-Host '=== 4. point admin confirms return-in (expect: item -> PUBLIC(1), claimedAt cleared) ==='
$r = Api 'Put' '/drop-points/1/items/31/return-in' $tP1 $null
Write-Host "return-in: $($r.code) $($r.message)"
Write-Host (ItemState 31 $tU1)

$tU3 = Login 'user3' '123456'
Write-Host '=== 5. user3 re-claims the same item (expect allowed, settlement works normally) ==='
$r = Api 'Post' '/claims' $tU3 @{ itemId = 31; answer = 'again' }
Write-Host "apply: $($r.code) $($r.message) claimId=$($r.data.id) status=$($r.data.applyStatus)"
$claimId = $r.data.id
$r = Api 'Put' "/claims/$claimId/approve" $tP1 $null
Write-Host "approve: $($r.code) $($r.message)"
$r = Api 'Put' "/claims/$claimId/pickup" $tU3 @{ pickupPhoto = 'demo2.jpg'; pickupSignature = 'Wang Wu' }
Write-Host "pickup: $($r.code) $($r.message)"
Write-Host (ItemState 31 $tU1)
foreach ($uid in 4, 5, 6) {
  Write-Host ("creditlog user" + $uid + " item31: " + (CreditLogs $uid $tAdmin))
  Write-Host (UserCredit $uid $tAdmin)
}

Write-Host '=== 6. RBAC check: normal user calling return-in must be rejected ==='
try {
  $r = Api 'Put' '/drop-points/1/items/31/return-in' $tU1 $null
  Write-Host "unexpected success: $($r.code) $($r.message)"
} catch {
  Write-Host "blocked as expected: $($_.Exception.Message)"
}
