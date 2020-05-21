import csv
import json
from itertools import groupby
from multiprocessing import Queue
from random import choice
from statistics import mean
from time import sleep
from typing import Dict, List

import pika
from requests import get

_BS_URL = 'http://localhost:80/'


def _genres() -> Dict[int, List[str]]:
    with open('./data/movie_genres.csv') as genres_file:
        reader = csv.reader(genres_file, delimiter='\t')
        next(reader)
        return {int(movie_id): list(map(lambda g: g[1].lower().replace('-', '_'), genres)) for movie_id, genres in groupby(reader, key=lambda r: r[0])}


def _ratings() -> List[tuple]:
    with open('./data/user_ratedmovies-timestamps.csv') as ratings_file:
        reader = csv.reader(ratings_file, delimiter='\t')
        next(reader)
        return sorted(map(lambda row: (int(row[0]), int(row[1]), float(row[2]), int(row[3])), map(tuple, reader)), key=lambda i: i[3], reverse=True)


_MOVIES_GENRES = _genres()
_USER_RATED_MOVIES = _ratings()
_EXCHANGE = 'bs_update'
_connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
_channel = _connection.channel()
_channel.exchange_declare(exchange=_EXCHANGE, exchange_type='fanout')


def _user():
    user_id, movie_id, rating, last_active = choice(_USER_RATED_MOVIES)
    genres = _MOVIES_GENRES[movie_id]

    user_response = get(url=f'{_BS_URL}user/{user_id}')
    if user_response.status_code == 404:
        rated_movies = [{
            "id": movie_id,
            "genre": choice(genres),  # TODO: use list
            "rating": rating
        }]
        average_rating = {
            genre: rating
            for genre in genres
        }
        return {
            "id": user_id,
            "averageRating": average_rating,
            "ratedMovies": rated_movies,
            "stats": {
                "lastActive": last_active
            }
        }
    elif user_response.status_code == 200:
        print(f'repeat for {user_id}')
        current_profile = json.loads(user_response.text)

        current_rated_movies: List[dict] = current_profile['ratedMovies']
        current_rating = list(filter(lambda m: m['id'] == movie_id, current_rated_movies))
        if len(current_rating) == 0:
            rated_movies = current_rated_movies + [{
                "id": movie_id,
                "genre": choice(genres),  # TODO: use list
                "rating": rating
            }]
        else:
            rated_movies = list(filter(lambda m: m['id'] != movie_id, current_rated_movies)) + [{
                "id": movie_id,
                "genre": choice(genres),  # TODO: use list
                "rating": rating
            }]

        average_rating = {
            genre: round(mean(list(map(lambda r: r['rating'], ratings))), 1)
            for genre, ratings in groupby(rated_movies, key=lambda m: m['genre'])
        }

        return {
            "id": user_id,
            "averageRating": average_rating,
            "ratedMovies": rated_movies,
            "stats": {
                "lastActive": last_active
            }
        }

    else:
        print(f'error response for user {user_id}: {user_response}')


def publish(ids_queue: Queue = None):
    for _ in range(1000):
        sent = publish_user()
        if ids_queue is not None:
            ids_queue.put(sent)
        sleep(.01)
    print('produce end')


def publish_user() -> int:
    event = _user()
    print(event)
    user_id = event["id"]
    _channel.basic_publish(exchange=_EXCHANGE, routing_key='', body=json.dumps(event))
    print(f'Sending update for user {user_id}')
    return user_id


if __name__ == '__main__':
    publish()
