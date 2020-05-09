from statistics import mean

import pika
from time import sleep, time
from random import randint, uniform, choice
import json
from itertools import groupby

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()
channel.exchange_declare(exchange='bs_update', exchange_type='fanout')

REFERENCE_PROFILE = {
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


def user():
    rated_movies = [{
        "id": randint(0, 10000),
        "genre": choice(list(REFERENCE_PROFILE['averageRating'].keys())),
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


def produce():
    for n in range(1000):
        event = user()
        print(event["id"])
        channel.basic_publish(exchange='bs_update',
                              routing_key='',
                              body=json.dumps(event))
        sleep(10)


if __name__ == '__main__':
    produce()
