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
    r = call("POST", "/auth/login", {"username": u, "password": "123456"})[1]
    return r["data"]["token"]

tp1, ta = login("pointadmin1"), login("admin")

print("=== 1. 站点回填事实（不带结论）→ 应只回填并转处理中")
s, r = call("PUT", "/disputes/5/point-check", {"note": "10:20 到图书馆服务台核对：货架 A 区无该保温杯，当班同学称未见；监控画面待管理员审批后调取"}, tp1)
print(s, r.get("code"), r.get("message"))

print("=== 2. 管理员手动勾选回滚：招领方投放分 +1、认领方认领分 +1（后者未发放应自动跳过）")
s, r = call("PUT", "/disputes/5/handle", {
    "approved": True,
    "rollbackItems": ["PUBLISH_CHECK", "CLAIMER"],
    "itemDispose": "VOIDED",
    "handlerNote": "依据规则：虚假投放（从未在架）→ 回滚发布人投放分 1 分；认领人未取件无 +1 可退；物品作废"
}, ta)
print(s, r.get("code"), r.get("message"))

print("=== 3. 管理员对已处理工单再处理 → 应拒绝")
s, r = call("PUT", "/disputes/5/handle", {"approved": True, "rollbackItems": [], "itemDispose": "NONE", "handlerNote": "重复"}, ta)
print(s, r.get("code"), r.get("message"))
