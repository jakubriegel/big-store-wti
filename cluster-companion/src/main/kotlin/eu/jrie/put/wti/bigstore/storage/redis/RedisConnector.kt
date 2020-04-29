package eu.jrie.put.wti.bigstore.storage.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import java.time.Duration

class RedisConnector (
    host: String
) {
    private val port = 6379
    private val client = run {
        logger.info("Connecting to Redis: $host:$port")
        RedisClient.create(RedisURI(host, port, Duration.ofSeconds(5)))
            .connect()
            .reactive()
    }

    fun get(key: String) = client.get(key).asFlow()

    suspend fun set(key: String, value: String) = client.set(key, value)
        .asFlow()
        .collect {
            if (it != "OK") throw RuntimeException("error setting redis value")
        }

    suspend fun expire(key: String, ttl: Long) {
        client.expire(key, ttl).awaitSingle()
    }

    private suspend fun ping() = client.ping()
        .awaitSingle()
        .let { it == "PONG" }

    companion object {
        suspend fun test(host: String) = RedisConnector(host).ping()

        private val logger = LoggerFactory.getLogger(RedisConnector::class.java)
    }
}