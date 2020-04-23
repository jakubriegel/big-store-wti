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
import io.ktor.response.respond
import io.ktor.response.respondText
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
    val jsonMapper = JsonMapper()
    val redis = RedisConnector(environment.config.property("storage.redis.host").getString())
    val cache = UserCacheRepository(redis, jsonMapper)
    val cassandra = CassandraConnector(environment.config.property("storage.cassandra.host").getString())
    val repository = UserRepository(cassandra, this)
    val service = UserCacheService(this, cache, repository)
    routing {
        route("/user") {
            accept(ContentType.Application.Json) {
                get("/full/{id}") {
                    val id = call.parameters["id"]!!.toInt()
                    val user = service.get(id).let { jsonMapper.write(it) }
                    call.respondText(user, ContentType.Application.Json)
                }
            }
            accept(ContentType.Text.CSV) {
                get("/full/{id}") {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }
            }
        }
    }
}
