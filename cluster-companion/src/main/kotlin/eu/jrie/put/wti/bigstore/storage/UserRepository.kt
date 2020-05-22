package eu.jrie.put.wti.bigstore.storage

import com.datastax.oss.driver.api.core.cql.Row
import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.User
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.time.Instant

@ObsoleteCoroutinesApi
class UserRepository (
    private val cassandra: CassandraConnector,
    private val scope: CoroutineScope
) {
    suspend fun findUser(id: Int) = with(scope) {
        Triple(
            async { cassandra.cql("SELECT * FROM user_avg WHERE user_id = $id LIMIT 1").toRatings() },
            async { cassandra.cql("SELECT * FROM user_rated_movies WHERE user_id = $id").toMovies() },
            async { cassandra.cql("SELECT * FROM user_stats WHERE user_id = $id LIMIT 1").toStats() }
        )
    } .let { (ratings, movies, stats) ->
        User(id, ratings.await(), movies.await(), stats.await())
    }

    private suspend fun Flow<Row>.toRatings() = map { row ->
        row.columnDefinitions
            .map { it.name }
            .filter { it.toString().startsWith("avg_") }
            .map { it.toString() to row.getFloat(it) }
            .map { (name, value) -> name.substringAfter("avg_") to value }
            .toTypedArray()
            .let { mapOf(*it) }
    } .single()

    private suspend fun Flow<Row>.toMovies() = map { row ->
        Triple(row.getInt("movie_id"), row.getList("genre", String::class.java) ?: emptyList(), row.getFloat("rating"))
    }.map { (id, genre, rating) ->
        Movie(id, genre, rating)
    }.toList()

    private suspend fun Flow<Row>.toStats() = map { row ->
        row.getInstant("last_active") ?: Instant.EPOCH
    }.map {
        UserStats(it)
    }.single()
}
