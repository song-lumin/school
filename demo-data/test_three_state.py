# -*- coding: utf-8 -*-
import json, urllib.request, urllib.error

BASE = "http://localhost/api"

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

tp1, tp2, ta = login("pointadmin1"), login("pointadmin2"), login("admin")

print("=== 1. 站点对 #102 处理：选「被他人拿走」→ 应结案(status=1)")
s, r = call("PUT", "/disputes/102/point-check", {"note": "现场核对货架无此物，监控显示已被他人取走", "outcome": "STOLEN"}, tp1)
print(s, r.get("code"), r.get("message"))
d = call("GET", "/disputes/102", token=tp1)[1].get("data") or {}
print("   status=", d.get("status"), "result=", d.get("handleResult") if "handleResult" in d else d.get("handle_result"))

print("=== 2. 站点对 #5 处理：选「其他」→ 应只回填并转处理中(status=3)")
s, r = call("PUT", "/disputes/5/point-check", {"note": "实物不在架，监控画面需管理员调取，站点无法自行定论", "outcome": "OTHER"}, tp1)
print(s, r.get("code"), r.get("message"))
d5 = call("GET", "/disputes/5", token=tp1)[1].get("data") or {}
print("   status=", d5.get("status"))

print("=== 3. 管理员对「处理中」的 #5 定责 → 应可处理")
s, r = call("PUT", "/disputes/5/handle", {"approved": True, "outcome": "NOT_DELIVERED", "handlerNote": "监控确认从未投放，按从未投放处理"}, ta)
print(s, r.get("code"), r.get("message"))
d5 = call("GET", "/disputes/5", token=ta)[1].get("data") or {}
print("   status=", d5.get("status"), "result=", d5.get("handleResult"))

print("=== 4. 站点对待处理的 #3（不符，本点位 pointadmin2）→ 应被类型守卫拦下")
s, r = call("PUT", "/disputes/3/point-check", {"note": "测试", "outcome": "STOLEN"}, tp2)
print(s, r.get("code"), r.get("message"))
