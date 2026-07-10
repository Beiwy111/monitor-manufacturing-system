#!/usr/bin/env python3
"""同步 Redis 中派工扩展信息，确保补派任务在前端可见。"""
import json
import subprocess
from datetime import datetime

REDIS_KEY = 'mes:runtime:state'


def load_redis_state():
    raw = subprocess.check_output(['redis-cli', '--raw', 'GET', REDIS_KEY])
    text = raw.decode('utf-8', errors='replace').strip()
    if not text or text == '(nil)':
        return {'inboundTasks': [], 'issueTasks': [], 'stockFlows': [], 'operationLogs': [], 'extras': {}, 'logSeq': 0}
    return json.loads(text)


def save_redis_state(state):
    payload = json.dumps(state, ensure_ascii=False).encode('utf-8')
    proc = subprocess.run(['redis-cli', '-x', 'SET', REDIS_KEY], input=payload, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.decode('utf-8', errors='replace') or proc.stdout.decode('utf-8', errors='replace'))


def fetch_dispatches():
    cmd = [
        'mysql', '-uroot', '-pZyd051104', '-N', '-B',
        '-e',
        """
        SELECT dt.dispatch_no, ps.step_name, IFNULL(u.username,''), IFNULL(u.real_name,''), IFNULL(e.equipment_name,'')
        FROM dispatch_task dt
        JOIN process_step ps ON ps.step_id = dt.step_id
        LEFT JOIN user u ON u.user_id = dt.operator_id
        LEFT JOIN equipment e ON e.equipment_id = dt.equipment_id
        ORDER BY dt.dispatch_id;
        """,
        'display_manufacturing',
    ]
    raw = subprocess.check_output(cmd, text=True, stderr=subprocess.DEVNULL)
    rows = []
    for line in raw.splitlines():
        parts = line.split('\t')
        if len(parts) >= 5:
            rows.append(parts[:5])
    return rows


def main():
    rows = fetch_dispatches()
    state = load_redis_state()
    extras = state.setdefault('extras', {})
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

    for dispatch_no, step_name, username, real_name, equipment_name in rows:
        key = f'dispatch:{dispatch_no}'
        item = extras.setdefault(key, {})
        item['processStep'] = step_name
        if username:
            item['operator'] = username
        if real_name:
            item['operatorName'] = real_name
        if equipment_name:
            item['equipment'] = equipment_name
        item.setdefault('planStart', now)

    save_redis_state(state)
    print(f'patched {len(rows)} dispatch extras in redis')


if __name__ == '__main__':
    main()
