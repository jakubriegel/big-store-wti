from multiprocessing import Process
from time import sleep
from typing import List, Set

from client.asynchronous.profiles import publish
from multiprocessing import Queue


def insert_rest(ids_queue: Queue) -> None:
    ids: Set[str] = set()
    for _ in range(1000):
        while ids_queue.qsize() != 0:
            ids.add(ids_queue.get_nowait())
        sleep(1)


def get_rest() -> None:
    print('get_rest')


def run() -> None:
    ids_queue = Queue(100)

    jobs = [
        Process(target=publish, args=(ids_queue, )),
        Process(target=insert_rest, args=(ids_queue, )),
        Process(target=get_rest)
    ]
    for p in jobs:
        p.start()

    for p in jobs:
        p.join()


if __name__ == '__main__':
    run()
