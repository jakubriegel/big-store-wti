import json
from multiprocessing import Queue
from statistics import mean
from typing import Set
from random import choice

from requests import get


def get_client(ids_queue: Queue) -> None:

    stats = {
        "status": {
            200: 0,
            404: 0,
            500: 0
        },
        "time": {
            200: [],
            404: [],
            500: []
        }
    }
    responses = []

    ids: Set[int] = set()
    ids.add(ids_queue.get())
    for _ in range(5000):
        while ids_queue.qsize() > 1:
            ids.add(ids_queue.get_nowait())

        user_id = choice(tuple(ids))
        response = get(
            url=f'http://localhost/user/{user_id}',
            headers={'Accept': 'application/json'}
        )

        responses.append(response)
        stats['status'][response.status_code] += 1
        # noinspection PyUnresolvedReferences
        stats['time'][response.status_code].append(response.elapsed.microseconds / 1000)

    print('get end')
    output = open('stats.json', 'w')
    output.write(json.dumps({
        'stats': stats,
        'avgResponseTime': mean(map(lambda r: r.elapsed.microseconds, responses)) / 1000
    }, indent=2))
