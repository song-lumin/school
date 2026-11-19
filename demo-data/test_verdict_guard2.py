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

tp2 = login("pointadmin2")
print("--- 本点位的不符工单(#3, item13 点位4) 用结论（应 400 拒绝） ---")
s, r = call("PUT", "/disputes/3/point-check", {"note": "实测", "outcome": "STOLEN"}, tp2)
print(s, r.get("code"), r.get("message"))
print("--- 同一工单选其他（应 200 只回填） ---")
s, r = call("PUT", "/disputes/3/point-check", {"note": "实测：不符类只回填", "outcome": "OTHER"}, tp2)
print(s, r.get("code"), r.get("message"))
