import json, urllib.request

BASE = 'http://localhost/api'

def req(path, token=None, method='GET', body=None):
    data = None
    headers = {'Content-Type': 'application/json'}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
    if token:
        headers['Authorization'] = 'Bearer ' + token
    r = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=15) as resp:
        return json.loads(resp.read().decode('utf-8'))

def summarize_list(path):
    res = req(path)
    d = res.get('data') or {}
    recs = d.get('records', []) if isinstance(d, dict) else []
    statuses = {}
    with_time = 0
    imgs = 0
    for r in recs:
        s = r.get('itemStatus')
        statuses[s] = statuses.get(s, 0) + 1
        if r.get('foundTimeText'):
            with_time += 1
        if r.get('images'):
            imgs += 1
    return len(recs), statuses, with_time, imgs

print('anon itemStatus=0 ->', summarize_list('/found-items?itemStatus=0&page=1&size=50'))
print('anon itemStatus=3 ->', summarize_list('/found-items?itemStatus=3&page=1&size=50'))
print('anon itemStatus=5 ->', summarize_list('/found-items?itemStatus=5&page=1&size=50'))
print('public default   ->', summarize_list('/found-items?page=1&size=50'))

login = req('/auth/login', method='POST', body={'username': 'user4', 'password': '123456'})
tok = (login.get('data') or {}).get('token')
me = req('/auth/me', token=tok).get('data') or {}
print('user4 me: creditScore=', me.get('creditScore'), 'misclaimCount=', me.get('misclaimCount'))

logs = req('/credits/logs', token=tok).get('data') or {}
recs = logs.get('records', []) if isinstance(logs, dict) else logs
zero = [x for x in recs if (x.get('changeAmount') == 0)]
mis = [x for x in recs if str(x.get('operationType', '')).upper().startswith('MISCLAIM')]
print('credit logs total=', len(recs), 'zero_amount=', len(zero), 'misclaim_rows=', len(mis))
