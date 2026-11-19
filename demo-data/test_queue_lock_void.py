# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"
ITEM = 5          # 保温杯，点位 1，发布人 user10(id 14)

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

t3 = login("user3")     # 第一个认领人 6
t2 = login("user2")     # 第二个认领人 5（排队）
t10 = login("user10")   # 发布人 14

print("=== 基线 ===")
print(q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM))
print(q("SELECT id,item_id,claimer_id,apply_status,reject_count FROM claim_apply WHERE item_id = %d;" % ITEM))

print("=== 1. user3 先认领 item5 ===")
s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "杯子是我的"}, t3)
print(s, str(r)[:80]); a1 = r["data"]["id"]

print("=== 2. user2 后认领 item5（应进入待审核）===")
s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "杯子是我的，杯底有贴纸"}, t2)
print(s, str(r)[:80]); a2 = r["data"]["id"]

print("=== 3. 发布人批准 user3 ===")
s, r = call("PUT", "/claims/%s/approve" % a1, None, t10)
print(s, str(r)[:80])
print(q("SELECT id,claimer_id,apply_status,reject_reason FROM claim_apply WHERE id IN (%s,%s);" % (a1, a2))
      + "   -- 期望：user3=4，user2=3(LOCKED)")

print("=== 4. user3 现场取消认领 ===")
s, r = call("PUT", "/claims/%s/cancel-pickup" % a1, None, t3)
print(s, str(r)[:110])
print(q("SELECT id,claimer_id,apply_status,reject_count,reject_reason FROM claim_apply WHERE id IN (%s,%s);" % (a1, a2))
      + "   -- 期望：user3=2，user2=2(作废) 而不是 0(待审核)")
print("item5:", q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM), "  -- 期望 1(公开)")

print("=== 5. 给 user2 的站内信 ===")
print(q("SELECT id,title FROM sys_message WHERE user_id = 5 ORDER BY id DESC LIMIT 3;"))
