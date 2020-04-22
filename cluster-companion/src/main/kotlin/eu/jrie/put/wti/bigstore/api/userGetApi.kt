package eu.jrie.put.wti.bigstore.api

import eu.jrie.put.wti.bigstore.service.UserCacheService
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
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
    val cassandra = CassandraConnector(environment.config.property("storage.cassandra.host").getString())
    val service = UserCacheService(environment.config.property("storage.redis.host").getString(), this, cassandra)
    routing {
        route("/user") {
            accept(ContentType.Application.Json) {
                get("/full/{id}") {
                    val id = call.parameters["id"]!!.toInt()
                    call.respondText(service.get(id), ContentType.Application.Json)
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
