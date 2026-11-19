# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"
ITEM = 6          # 校园卡（已遮挡姓名），点位 3，发布人 user3(id 6)
POINT = 3

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

t2 = login("user2")        # 认领人 5
t3 = login("user3")        # 发布人 6
t1 = login("user1")        # 第二位认领人 4
ta = login("admin")
tp = login("pointadmin1")

print("=== 0. 清理上轮脏工单 104 ===")
q("DELETE FROM dispute WHERE id = 104;")

print("=== 基线 ===")
print(q("SELECT id,change_amount,operation_type,related_item_id,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 2;"))
print("item6:", q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM))
print(q("SELECT id,credit_score FROM user WHERE id IN (4,5,6);"))

print("=== 1. user2 认领 item6 ===")
s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "卡是我的，姓名是我本人"}, t2)
print("apply:", s, str(r)[:90])
aid = r["data"]["id"]
print("apply id:", aid)

print("=== 2. 发布人 user3 批准 ===")
s, r = call("PUT", "/claims/%s/approve" % aid, None, t3)
print("approve:", s, str(r)[:90])

print("=== 3. user2 取件（应发 发布人+3、认领人+1）===")
s, r = call("PUT", "/claims/%s/pickup" % aid, {"pickupPhoto": "https://example.com/c1.jpg", "pickupSignature": "李思雨"}, t2)
print("pickup:", s, str(r)[:90])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 3;"))
print(q("SELECT id,credit_score FROM user WHERE id IN (5,6);"))

print("=== 4. user2 发起「物品不符」===")
s, r = call("POST", "/disputes", {"applyId": aid, "disputeType": "ITEM_MISMATCH", "description": "到站发现不是我的卡（实测不符回滚）"}, t2)
print("create:", s, str(r)[:90])
did = r["data"]["id"]
print("dispute id:", did)

print("=== 5. 管理员通过（发布人 +3/+1 应回滚）===")
s, r = call("PUT", "/disputes/%s/handle" % did, {"approved": True, "handlerNote": "实测：不符，成交无效"}, ta)
print("handle:", s, str(r)[:90])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,reason FROM credit_log ORDER BY id DESC LIMIT 4;"))
print("item6:", q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM))
print(q("SELECT id,credit_score,misclaim_count FROM user WHERE id IN (5,6);"))

print("=== 6. 站点确认归还入库 ===")
s, r = call("PUT", "/drop-points/%d/items/%d/return-in" % (POINT, ITEM), None, tp)
print("returnIn:", s, str(r)[:110])
print("item6:", q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM))

print("=== 7. user1 重新认领 item6 ===")
s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "确认是我的校园卡"}, t1)
print("apply:", s, str(r)[:90])
aid2 = r["data"]["id"]
print("new apply id:", aid2)

print("=== 8. 发布人 user3 批准 ===")
s, r = call("PUT", "/claims/%s/approve" % aid2, None, t3)
print("approve:", s, str(r)[:90])

print("=== 9. user1 取件（发布人 +3 应重新发放）===")
s, r = call("PUT", "/claims/%s/pickup" % aid2, {"pickupPhoto": "https://example.com/c2.jpg", "pickupSignature": "张明"}, t1)
print("pickup:", s, str(r)[:90])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,reason FROM credit_log ORDER BY id DESC LIMIT 6;"))
print(q("SELECT id,credit_score FROM user WHERE id IN (4,6);"))
print("item6:", q("SELECT item_status FROM found_item WHERE id = %d;" % ITEM))
