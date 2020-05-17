from statistics import mean

import pika
from time import sleep, time
from random import randint, uniform, choice
import json
from itertools import groupby
from multiprocessing import Queue

_EXCHANGE = 'bs_update'
_connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
_channel = _connection.channel()
_channel.exchange_declare(exchange=_EXCHANGE, exchange_type='fanout')

_REFERENCE_PROFILE = {
    "id": 1,
    "averageRating": {
        "action": 0.0,
        "adventure": 0.0,
        "animation": 0.0,
        "children": 0.0,
        "comedy": 0.0,
        "crime": 0.0,
        "documentary": 0.0,
        "drama": 0.0,
        "fantasy": 0.0,
        "film_noir": 0.0,
        "horror": 0.0,
        "imax": 0.0,
        "musical": 0.0,
        "mystery": 0.0,
        "romance": 0.0,
        "sci_fi": 0.0,
        "short": 0.0,
        "thriller": 0.0,
        "war": 0.0,
        "western": 0.0
    },
    "ratedMovies": [
        {
            "id": 1,
            "genre": "action",
            "rating": 0.0
        }
    ],
    "stats": {
        "lastActive": 1588615950.216000000
    }
}


def _user():
    rated_movies = [{
        "id": randint(0, 10000),
        "genre": choice(list(_REFERENCE_PROFILE['averageRating'].keys())),
        "rating": round(uniform(0.0, 10.0), 1)
    } for _ in range(randint(1, 100))]

    average_rating = {
        genre: round(mean(list(map(lambda r: r['rating'], ratings))), 1)
        for genre, ratings in groupby(rated_movies, key=lambda m: m['genre'])
    }

    return {
        "id": randint(0, 1000),
        "averageRating": average_rating,
        "ratedMovies": rated_movies,
        "stats": {
            "lastActive": int(time()) - randint(1, 30)
        }
    }


def publish(ids_queue: Queue = None):
    for _ in range(1000):
        sent = publish_user()
        if ids_queue is not None:
            ids_queue.put(sent)
        sleep(.01)
    print('produce end')


def publish_user() -> int:
    event = _user()
    user_id = event["id"]
    _channel.basic_publish(exchange=_EXCHANGE, routing_key='', body=json.dumps(event))
    print(f'Sending update for user {user_id}')
    return user_id


if __name__ == '__main__':
    publish()
