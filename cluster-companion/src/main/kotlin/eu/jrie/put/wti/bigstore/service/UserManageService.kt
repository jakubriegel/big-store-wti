package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.slf4j.LoggerFactory

@ObsoleteCoroutinesApi
class UserManageService (
    private val cassandra: CassandraConnector
) {

    suspend fun updateUserAverageRatings(userId: Int, averageRating: Map<String, Float>) {
        logger.debug("Updating average ratings for user $userId $averageRating")
        val sets = averageRating.map { it.toPair() }
            .joinToString(", ") { (name, avg) -> "avg_$name = $avg" }
        cassandra.cql("UPDATE user_avg SET $sets WHERE user_id = $userId")
    }

    suspend fun updateUserRatedMovies(userId: Int, movies: List<Movie>) {
        logger.debug("Updating rated movies for user $userId $movies")
        movies.forEach {
            cassandra.cql("INSERT INTO user_rated_movies (user_id, movie_id, genre, rating) VALUES ($userId, ${it.id}, '${it.genre}', ${it.rating})")
        }
    }

    suspend fun updateUserStats(userId: Int, stats: UserStats) {
        logger.debug("Updating stats for user $userId $stats")
        cassandra.cql("UPDATE user_stats SET last_active = ${stats.lastActive.toEpochMilli()} WHERE user_id = $userId")
    }

    suspend fun deleteUser(userId: Int) {
        logger.debug("Deleting user $userId")
        cassandra.cql("DELETE FROM user_avg WHERE user_id = $userId")
        cassandra.cql("DELETE FROM user_rated_movies WHERE user_id = $userId")
        cassandra.cql("DELETE FROM user_stats WHERE user_id = $userId")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserManageService::class.java)
    }
}