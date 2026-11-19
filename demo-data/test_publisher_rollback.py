# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"

def q(sql):
    p = subprocess.run(["docker", "exec", "school-mysql", "mysql", "-uroot", "-proot123456",
                        "--default-character-set=utf8mb4", "-N", "-e", "USE school_lost_found; " + sql],
                       capture_output=True)
    return p.stdout.decode("utf-8", "replace")

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

t2 = login("user2")
ta = login("admin")

print("=== 1. 取件 apply1（item9，发布人 user9）===")
s, r = call("PUT", "/claims/1/pickup", {"pickupPhoto": "https://example.com/p.jpg", "pickupSignature": "李思雨"}, t2)
print("pickup:", s, str(r)[:120])

print("=== 2. 取件后的积分流水 ===")
print(q("SELECT id,user_id,change_amount,operation_type,related_item_id,related_apply_id FROM credit_log WHERE id>144 ORDER BY id;"))
print(q("SELECT id,username,credit_score FROM user WHERE id IN (5,9);"))

print("=== 3. user2 发起 OTHER 纠纷 ===")
s, r = call("POST", "/disputes", {"applyId": 1, "disputeType": "OTHER", "description": "线下核验后确认本次成交无效（测试）"}, t2)
print("create:", s, str(r)[:160])
did = None
try:
    did = r["data"]["id"]
except Exception:
    pass
print("dispute id:", did)
print(q("SELECT id,apply_id,item_id,dispute_type,status FROM dispute ORDER BY id DESC LIMIT 1;"))

print("=== 4. 管理员通过（OTHER 分支应回滚发布人 +3/+1）===")
s, r = call("PUT", "/disputes/%s/handle" % did, {"approved": True, "handlerNote": "测试：其他纠纷判作废"}, ta)
print("handle:", s, str(r)[:160])

print("=== 5. 回滚结果 ===")
print(q("SELECT id,user_id,change_amount,operation_type,related_item_id,related_apply_id,reason FROM credit_log WHERE id>144 ORDER BY id;"))
print(q("SELECT id,username,credit_score FROM user WHERE id IN (5,9);"))
print(q("SELECT id,item_status,takedown_reason FROM found_item WHERE id=9;"))
