from multiprocessing import Process
from multiprocessing import Queue

from client.asynchronous.profiles import publish
from client.rest.get import get_client


def insert_rest() -> None:
    print('insert_rest')


def run() -> None:
    ids_queue = Queue(100)

    jobs = [
        Process(target=publish, args=(ids_queue, )),
        Process(target=get_client, args=(ids_queue, )),
        Process(target=insert_rest)
    ]
    for p in jobs:
        p.start()

    for p in jobs:
        p.join()


if __name__ == '__main__':
    run()
