# -*- coding: utf-8 -*-
import json, subprocess, urllib.request, urllib.error

BASE = "http://localhost/api"
ITEM = 2  # 白色无线耳机，点位 2，发布人 user5(id 9)，转发人 user2(id 5)

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

t3, t5, t2, ta = login("user3"), login("user5"), login("user2"), login("admin")

print("=== 基线 ===")
print("item2:", q("SELECT item_status FROM found_item WHERE id=%d;" % ITEM))
print("分数(3认领人/5发布人/2转发人):", q("SELECT id,credit_score FROM user WHERE id IN (2,5,9,6);"))
print("log max:", q("SELECT MAX(id) FROM credit_log;"))

s, r = call("POST", "/claims", {"itemId": ITEM, "answer": "白色耳机，盒子有轻微划痕"}, t3)
print("认领:", s, str(r)[:60]); aid = r["data"]["id"]
s, r = call("PUT", "/claims/%s/approve" % aid, None, t5)
print("批准:", s, str(r)[:60])
s, r = call("PUT", "/claims/%s/pickup" % aid, {"pickupPhoto": "https://example.com/a.jpg", "pickupSignature": "王浩"}, t3)
print("取件:", s, str(r)[:60])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id FROM credit_log ORDER BY id DESC LIMIT 4;"))
print("分数:", q("SELECT id,credit_score FROM user WHERE id IN (2,5,9,6);"))

s, r = call("POST", "/disputes", {"applyId": aid, "disputeType": "ITEM_DAMAGED", "description": "实测 sheet2：损坏·站点责，转发 +1 应不动"}, t3)
print("损坏工单:", s, str(r)[:60]); did = r["data"]["id"]
s, r = call("PUT", "/disputes/%s/handle" % did, {"approved": True, "handlerNote": "实测站点责", "outcome": "SITE_FAULT"}, ta)
print("定责:", s, str(r)[:60])
print(q("SELECT id,user_id,change_amount,operation_type,related_apply_id,LEFT(reason,24) FROM credit_log ORDER BY id DESC LIMIT 4;"))
print("分数:", q("SELECT id,credit_score FROM user WHERE id IN (2,5,9,6);"))
print("item2:", q("SELECT item_status FROM found_item WHERE id=%d;" % ITEM))
