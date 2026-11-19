# -*- coding: utf-8 -*-
import json
import urllib.request

BASE = 'http://localhost/api'


def call(path, method='GET', token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body else None
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')


st, res = call('/auth/login', 'POST', None, {'username': 'admin', 'password': '123456'})
token = res['data']['token']
print('admin login', st)

st, res = call('/chat/messages', 'POST', token,
               {'disputeId': 103, 'content': '我是系统管理员陈志远：这条工单已核实物品确在站点被错放货架，站点周敏已把实物归位，请在 3 日内到 3 号宿舍区服务站凭证件领取，逾期按规则处理。'})
print('admin send', st, json.dumps(res, ensure_ascii=False)[:200])

st, res = call('/chat/messages?disputeId=103', 'GET', token)
rows = res['data']
print('thread rows', len(rows))
for r in rows:
    print(' -', r['senderName'], r['senderRole'], r['content'][:40])
