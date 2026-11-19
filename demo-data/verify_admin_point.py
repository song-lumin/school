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
    d = r["data"]
    role = d.get("role") or (d.get("user") or {}).get("role") or "?"
    return d["token"], role

for name in ("admin", "pointadmin1", "pointadmin2"):
    tok, role = login(name)
    s, r = call("GET", "/disputes/point?page=1&size=50", None, tok)
    data = r.get("data") if isinstance(r, dict) else None
    recs = (data or {}).get("records", []) if isinstance(data, dict) else []
    total = (data or {}).get("total") if isinstance(data, dict) else r
    print(f"{name:12s} role={role:10s} http={s} total={total} ids={[x.get('id') for x in recs]}")

tok, _ = login("admin")
s, r = call("GET", "/drop-points", None, tok)
pts = r.get("data", []) if isinstance(r, dict) else []
print("admin 可切换点位:", [(p.get("id"), p.get("name")) for p in pts])
