DROP KEYSPACE IF EXISTS users;
CREATE KEYSPACE users
WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

USE users;

DROP TABLE IF EXISTS  user_avg;
CREATE TABLE user_avg (
    user_id int,
    avg_action float,
    avg_adventure float,
    avg_animation float,
    avg_children float,
    avg_comedy float,
    avg_crime float,
    avg_documentary float,
    avg_drama float,
    avg_fantasy float,
    avg_film_noir float,
    avg_horror float,
    avg_imax float,
    avg_musical float,
    avg_mystery float,
    avg_romance float,
    avg_sci_fi float,
    avg_short float,
    avg_thriller float,
    avg_war float,
    avg_western float,

    PRIMARY KEY (user_id)
);

DROP TABLE IF EXISTS  user_rated_movies;
CREATE TABLE user_rated_movies (
    user_id int,
    movie_id int,
    genre list<text>,
    rating float,

    PRIMARY KEY (user_id, movie_id)
);

DROP TABLE IF EXISTS  user_stats;
CREATE TABLE user_stats (
    user_id int,
    last_active timestamp,

    PRIMARY KEY (user_id)
);

