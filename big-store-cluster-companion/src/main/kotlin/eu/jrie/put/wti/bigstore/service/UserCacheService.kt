
package eu.jrie.put.wti.bigstore.service

import com.datastax.oss.driver.api.core.cql.Row
import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UpdateCacheTask
import eu.jrie.put.wti.bigstore.model.User
import eu.jrie.put.wti.bigstore.model.UserCache
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import eu.jrie.put.wti.bigstore.util.JsonMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class UserCacheService (
    redisHost: String,
    private val scope: CoroutineScope,
    private val cassandra: CassandraConnector
) {
    private val redis = run {
        val client = RedisClient.create(RedisURI(redisHost, 6379, Duration.ofSeconds(5)))
        val conn = client.connect()
        conn.reactive()
    }

    @ObsoleteCoroutinesApi
    private val mapper = JsonMapper()

    private val updateBuffer = Channel<UpdateCacheTask>(capacity = Channel.UNLIMITED)

    init {
        scope.launch {
            logger.info("started db fetch job")
            updateBuffer.consumeEach { task ->
                fetchUserFromDb(task.userId)
                    .also { logger.info("fetched from db $it") }
                    .also { task.complete(it) }
                    .also { setCache(it) }
            }
        }
    }

    suspend fun get(id: Int): String {
        return redis.get("user-$id")
            .defaultIfEmpty("")
            .asFlow()
            .map {
                if (it.isEmpty()) fetchUserFromDb(id)
                else getUpToDate(mapper.read(it))
            }
            .single()
            .let { mapper.write(it) }
    }

    private suspend fun fetchUserFromDb(id: Int) = with(scope) {
        Triple(
            async { cassandra.cql("SELECT * FROM user_avg WHERE user_id = $id LIMIT 1").toRatings() },
            async { cassandra.cql("SELECT * FROM user_rated_movies WHERE user_id = $id").toMovies() },
            async { cassandra.cql("SELECT * FROM user_stats WHERE user_id = $id LIMIT 1").toStats() }
        )
    } .let { (ratings, movies, stats) ->
        User(id, ratings.await(), movies.await(), stats.await())
    } .also {
        setCache(it)
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
        Triple(row.getInt("movie_id"), row.getString("genre") ?: "", row.getFloat("rating"))
    }.map { (id, genre, rating) ->
        Movie(id, genre, rating)
    }.toList()

    private suspend fun Flow<Row>.toStats() = map { row ->
        row.getInstant("last_active") ?: Instant.EPOCH
    }.map {
        UserStats(it)
    }.single()

    private suspend fun getUpToDate(cached: UserCache) = when {
        cached.isUpToDate() -> {
            logger.info("returning user from cache ${cached.user}")
            cached.user
        }
        else -> tryUpdate(cached.user)
    }

    private suspend fun tryUpdate(user: User) = UpdateCacheTask(user.id).let {
        updateBuffer.send(it)
        withTimeoutOrNull(185) {
            it.await().also { u -> logger.info("returning user from db $u") }
        } ?: user.also { u -> logger.info("returning user from cache after timeout $u") }
    }

    private fun UserCache.isUpToDate() = updatedAt.isAfter(now() - Duration.of(5, ChronoUnit.SECONDS))

    private suspend fun setCache(user: User) {
        redis.set("user-${user.id}", mapper.write(UserCache(user, now()))).awaitSingle()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserCacheService::class.java)
    }
}
