#!/usr/bin/env python3
"""清理 Redis 中阻断终检提交的质检/入库残留状态。"""
import json
import subprocess

REDIS_KEY = 'mes:runtime:state'
WORK_ORDER_NO = 'WO202607001'


def load_state():
    raw = subprocess.check_output(['redis-cli', '--raw', 'GET', REDIS_KEY])
    text = raw.decode('utf-8', errors='replace').strip()
    if not text or text == '(nil)':
        return {
            'inboundTasks': [], 'issueTasks': [], 'stockFlows': [],
            'operationLogs': [], 'extras': {}, 'logSeq': 0,
        }
    return json.loads(text)


def save_state(state):
    payload = json.dumps(state, ensure_ascii=False).encode('utf-8')
    proc = subprocess.run(['redis-cli', '-x', 'SET', REDIS_KEY], input=payload, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.decode('utf-8', errors='replace'))


def main():
    state = load_state()
    extras = state.setdefault('extras', {})

    for key in list(extras.keys()):
        if key.startswith('inspection:'):
            del extras[key]

    state['inboundTasks'] = [
        t for t in state.get('inboundTasks', [])
        if not str(t.get('workOrderId', '')).endswith(WORK_ORDER_NO)
        and not str(t.get('refNo', '')).startswith('QI')
    ]

    save_state(state)
    print(f'cleaned redis workflow state for {WORK_ORDER_NO}')


if __name__ == '__main__':
    main()
