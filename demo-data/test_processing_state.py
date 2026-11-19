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

tp2 = login("pointadmin2")
print("=== #3（不符，本点位）站点选「其他」→ 应转为处理中")
s, r = call("PUT", "/disputes/3/point-check", {"note": "现场没找到对应实物，需管理员按凭证进一步核对", "outcome": "OTHER"}, tp2)
print(s, r.get("code"), r.get("message"))
print("=== 再提交一次（处理中应允许继续处理，不报已处理）===")
s, r = call("PUT", "/disputes/3/point-check", {"note": "补充：监控回放已申请调取", "outcome": "OTHER"}, tp2)
print(s, r.get("code"), r.get("message"))
