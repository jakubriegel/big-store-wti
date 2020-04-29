package eu.jrie.put.wti.bigstore.api

import eu.jrie.put.wti.bigstore.service.UserCacheService
import eu.jrie.put.wti.bigstore.storage.UserCacheRepository
import eu.jrie.put.wti.bigstore.storage.UserRepository
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import eu.jrie.put.wti.bigstore.storage.redis.RedisConnector
import eu.jrie.put.wti.bigstore.util.JsonMapper
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respond
import io.ktor.routing.accept
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@KtorExperimentalAPI
fun Application.userGetApi() {
    val service = userCacheService()
    routing {
        route("/user/{id}") {
            accept(ContentType.Application.Json) {
                get {
                    val user = service.get(userId)
                    if (user == null ) call.respond(NotFound)
                    else call.respond(user)
                }
            }
            accept(ContentType.Text.CSV) {
                get {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }
            }
        }
    }
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@KtorExperimentalAPI
private fun Application.userCacheService(): UserCacheService {
    val bestBefore = environment.config.property("big-store.cache.bestBefore").getString().toLong()
    val storeTimeout = environment.config.property("big-store.cache.storeTimeout").getString().toLong()

    val redis = RedisConnector(environment.config.property("big-store.cache.redis.host").getString())
    val cache = UserCacheRepository(
        redis,
        JsonMapper(),
        environment.config.property("big-store.cache.ttl").getString().toLong()
    )
    val cassandra = CassandraConnector(environment.config.property("big-store.store.cassandra.host").getString())
    val repository = UserRepository(cassandra, this)

    return UserCacheService(
        bestBefore, storeTimeout,
        this, cache, repository
    )
}
