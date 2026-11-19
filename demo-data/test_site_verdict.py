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

tp1 = login("pointadmin1")
ta = login("admin")

print("=== 基线 ===")
print("工单 102:", q("SELECT id,item_id,apply_id,dispute_type,status,handle_result FROM dispute WHERE id=102;"))
print("item100:", q("SELECT id,item_status,drop_point_id,founder_id FROM found_item WHERE id=100;"))
print("工单 5:", q("SELECT id,item_id,apply_id,dispute_type,status FROM dispute WHERE id=5;"))
print("dispute 待处理数:", q("SELECT COUNT(*) FROM dispute WHERE status=0;"))

print("\n=== A. 站点对 102 选「被他人拿走」STOLEN（应直接结案）===")
s, r = call("PUT", "/disputes/102/point-check",
            {"note": "实测：宿舍区服务站货架未找到该物，监控显示他人取走", "outcome": "STOLEN"}, tp1)
print(s, str(r)[:100])
print("工单 102:", q("SELECT status,handle_result,LEFT(point_check_note,20),site_unresponsive FROM dispute WHERE id=102;"))
print("item100:", q("SELECT item_status FROM found_item WHERE id=100;"))
print("积分流水新增:", q("SELECT COUNT(*) FROM credit_log WHERE related_apply_id=100;"))

print("\n=== B. 站点对「物品不符」工单用结论（应被拒）===")
s, r = call("PUT", "/disputes/5/point-check", {"note": "实测", "outcome": "STOLEN"}, tp1)
print(s, str(r)[:120])

print("\n=== C. 站点对 5 选「其他」（应只回填、不改状态）===")
s, r = call("PUT", "/disputes/5/point-check", {"note": "实测：现场无法判定，等监控回放", "outcome": "OTHER"}, tp1)
print(s, str(r)[:100])
print("工单 5:", q("SELECT status,LEFT(point_check_note,24) FROM dispute WHERE id=5;"))
print("admin 收到的转派消息:", q("SELECT COUNT(*) FROM sys_message WHERE user_id=1 AND title LIKE '%无法定论%';"))
