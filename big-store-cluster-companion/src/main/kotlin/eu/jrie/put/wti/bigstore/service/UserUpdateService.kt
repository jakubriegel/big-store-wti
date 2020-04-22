package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.slf4j.LoggerFactory

@ObsoleteCoroutinesApi
class UserUpdateService (
    private val cassandra: CassandraConnector
) {

    suspend fun updateUserAverageRatings(userId: Int, averageRating: Map<String, Float>) {
        logger.info("updating average ratings for user $userId $averageRating")
        val sets = averageRating.map { it.toPair() }
            .joinToString(", ") { (name, avg) -> "avg_$name = $avg" }
        cassandra.cql("UPDATE user_avg SET $sets WHERE user_id = $userId")
    }

    suspend fun addUserRatedMovie(userId: Int, movie: Movie) {
        logger.info("updating rated movies for user $userId $movie")
        cassandra.cql("INSERT INTO user_rated_movies (user_id, movie_id, genre, rating) VALUES ($userId, ${movie.id}, '${movie.genre}', ${movie.rating})")
    }

    suspend fun updateUserStats(userId: Int, stats: UserStats) {
        logger.info("updating stats for user $userId $stats")
        cassandra.cql("UPDATE user_stats SET last_active = ${stats.lastActive.toEpochMilli()} WHERE user_id = $userId")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserUpdateService::class.java)
    }
}