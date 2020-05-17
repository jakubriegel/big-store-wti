from multiprocessing import Process
from client.asynchronous.profiles import produce


def insert_async() -> None:
    produce()


def insert_rest() -> None:
    print('insert_rest')


def get_rest() -> None:
    print('get_rest')


def run() -> None:
    jobs = [
        Process(target=insert_async),
        Process(target=insert_rest),
        Process(target=get_rest)
    ]
    for p in jobs:
        p.start()

    for p in jobs:
        p.join()


if __name__ == '__main__':
    run()
