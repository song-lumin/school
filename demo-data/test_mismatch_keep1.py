# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"
ITEM = 1   # 雨伞，点位 1，发布人 user1(id 4)，已有交物 +1 (log 101)

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

t3 = login("user3")   # 认领人 6
t1 = login("user1")   # 发布人 4
ta = login("admin")

print("=== 基线 ===")
print("item1:", q("SELECT item_status FROM found_item WHERE id=%d;" % ITEM))
print(q("SELECT id, credit_score FROM user WHERE id IN (4,6);"))
print(q("SELECT id,change_amount,operation_type,related_item_id,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 2;"))

print("=== 1. user3 认领 item1 ===")
s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "黑伞，伞柄有划痕"}, t3)
print(s, str(r)[:80]); aid = r["data"]["id"]

print("=== 2. 发布人 user1 批准 ===")
s, r = call("PUT", "/claims/%s/approve" % aid, None, t1)
print(s, str(r)[:80])

print("=== 3. user3 取件（发布人 +3、认领人 +1）===")
s, r = call("PUT", "/claims/%s/pickup" % aid, {"pickupPhoto": "https://example.com/u1.jpg", "pickupSignature": "王浩"}, t3)
print(s, str(r)[:80])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 3;"))
print(q("SELECT id, credit_score FROM user WHERE id IN (4,6);"))

print("=== 4. user3 发起「物品不符」===")
s, r = call("POST", "/disputes", {"applyId": aid, "disputeType": "ITEM_MISMATCH", "description": "实测：不符时应只回滚3分，保留投放1分"}, t3)
print(s, str(r)[:80]); did = r["data"]["id"]

print("=== 5. 管理员通过 ===")
s, r = call("PUT", "/disputes/%s/handle" % did, {"approved": True, "handlerNote": "实测 v2 规则"}, ta)
print(s, str(r)[:80])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,reason FROM credit_log ORDER BY id DESC LIMIT 4;"))
print(q("SELECT id, credit_score FROM user WHERE id IN (4,6);"), "  -- 期望发布人 = 基线(23)+3-3 = 23，1 分保留")
print("item1:", q("SELECT item_status FROM found_item WHERE id=%d;" % ITEM))
