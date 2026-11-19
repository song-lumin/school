# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"

def q(sql):
    p = subprocess.run(["docker", "exec", "school-mysql", "mysql", "-uroot", "-proot123456",
                        "--default-character-set=utf8mb4", "-N", "-e", "USE school_lost_found; " + sql],
                       capture_output=True)
    return p.stdout.decode("utf-8", "replace").strip()

def call(method, path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json;charset=utf-8")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")

def login(u):
    s, r = call("POST", "/auth/login", {"username": u, "password": "123456"})
    return r["data"]["token"]

t2 = login("user2")       # 认领人 5
tu1 = login("user1")      # 第二位认领人 4
t9 = login("user9")       # 发布人 13
ta = login("admin")
tp = login("pointadmin1")

print("=== 基线 ===")
print(q("SELECT id,change_amount,operation_type,related_item_id,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 3;"))
print("item9:", q("SELECT item_status FROM found_item WHERE id=9;"))

print("=== 1. user2 取件 apply1 ===")
s, r = call("PUT", "/claims/1/pickup", {"pickupPhoto": "https://example.com/p.jpg", "pickupSignature": "李思雨"}, t2)
print("pickup:", s, str(r)[:100])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 3;"))
print(q("SELECT id,credit_score FROM user WHERE id IN (5,13);"))

print("=== 2. user2 发起「物品不符」===")
s, r = call("POST", "/disputes", {"applyId": 1, "disputeType": "ITEM_MISMATCH", "description": "取件后发现不是我的钱包（测试不符回滚）"}, t2)
print("create:", s, str(r)[:140])
did = r["data"]["id"] if isinstance(r, dict) else None
print("dispute id:", did)

print("=== 3. 管理员通过（不符：发布人 +3/+1 应回滚）===")
s, r = call("PUT", "/disputes/%s/handle" % did, {"approved": True, "handlerNote": "测试：不符，成交无效"}, ta)
print("handle:", s, str(r)[:140])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,reason FROM credit_log ORDER BY id DESC LIMIT 5;"))
print("item9:", q("SELECT item_status FROM found_item WHERE id=9;"))
print(q("SELECT id,credit_score,misclaim_count FROM user WHERE id IN (5,13);"))

print("=== 4. 站点确认归还入库（物品应回到公开）===")
s, r = call("PUT", "/drop-points/1/items/9/return-in", None, tp)
print("returnIn:", s, str(r)[:140])
print("item9:", q("SELECT item_status FROM found_item WHERE id=9;"))

print("=== 5. user1 重新认领 item9 ===")
s, r = call("POST", "/claims", {"itemId": 9, "answer": "测试：重新认领"}, tu1)
print("apply:", s, str(r)[:140])
new_apply = r["data"]["id"] if isinstance(r, dict) else None
print("new apply id:", new_apply)

print("=== 6. 发布人 user9 批准 ===")
s, r = call("PUT", "/claims/%s/approve" % new_apply, None, t9)
print("approve:", s, str(r)[:120])

print("=== 7. user1 取件（发布人 +3 应重新发放）===")
s, r = call("PUT", "/claims/%s/pickup" % new_apply, {"pickupPhoto": "https://example.com/p2.jpg", "pickupSignature": "张明"}, tu1)
print("pickup:", s, str(r)[:100])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,reason FROM credit_log ORDER BY id DESC LIMIT 5;"))
print(q("SELECT id,credit_score FROM user WHERE id IN (4,13);"))
print("item9:", q("SELECT item_status FROM found_item WHERE id=9;"))
