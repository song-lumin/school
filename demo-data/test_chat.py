# -*- coding: utf-8 -*-
import json
import time
import urllib.request
import urllib.error

BASE = 'http://localhost/api'


def call(path, method='GET', token=None, body=None):
    data = None
    headers = {'Content-Type': 'application/json'}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8')
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw


def login(u, p='123456'):
    st, res = call('/auth/login', 'POST', None, {'username': u, 'password': p})
    if st != 200:
        raise SystemExit('login fail %s %s %s' % (u, st, res))
    data = res.get('data') or {}
    return data.get('token') or data.get('accessToken')


# 等后端就绪
for i in range(20):
    st, _ = call('/items?page=1&size=1')
    if st in (200, 401):
        break
    time.sleep(2)

t_user = login('user1')
t_point = login('pointadmin1')
t_admin = login('admin')
t_user2 = login('user2')
print('tokens ok')

print('\n[1] user1 读取工单102 沟通记录')
st, res = call('/chat/messages?disputeId=102', 'GET', t_user)
print(st, json.dumps(res, ensure_ascii=False)[:300])

print('\n[2] user1 发一条留言')
st, res = call('/chat/messages', 'POST', t_user, {'disputeId': 102, 'content': '19:20 左右我在 3 宿舍区服务站取杯子，柜子里没有，能看下监控吗？'})
print(st, json.dumps(res, ensure_ascii=False)[:300])

print('\n[3] pointadmin1 读取同一工单')
st, res = call('/chat/messages?disputeId=102', 'GET', t_point)
print(st, json.dumps(res, ensure_ascii=False)[:400])

print('\n[4] pointadmin1 回复')
st, res = call('/chat/messages', 'POST', t_point, {'disputeId': 102, 'content': '监控已调取，19:12 有人取走了一只深蓝保温杯，我这边把那一段截图交给系统管理员了。'})
print(st, json.dumps(res, ensure_ascii=False)[:300])

print('\n[5] 无关用户 user2 越权读取（应 403）')
st, res = call('/chat/messages?disputeId=102', 'GET', t_user2)
print(st, json.dumps(res, ensure_ascii=False)[:300])

print('\n[6] 我的沟通列表')
for name, tok in [('user1', t_user), ('pointadmin1', t_point), ('admin', t_admin)]:
    st, res = call('/chat/threads', 'GET', tok)
    rows = (res.get('data') if isinstance(res, dict) else None) or []
    print(name, st, 'threads=', len(rows), json.dumps(rows[:2], ensure_ascii=False)[:300])
