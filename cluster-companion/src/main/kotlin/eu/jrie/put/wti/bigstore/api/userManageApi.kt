package eu.jrie.put.wti.bigstore.api

import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.service.UserManageService
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ObsoleteCoroutinesApi

@KtorExperimentalAPI
@ObsoleteCoroutinesApi
fun Application.userManageApi() {
    val cassandra = CassandraConnector(environment.config.property("big-store.storage.cassandra.host").getString())
    val service = UserManageService(cassandra)
    routing {
        route("/user/{id}") {
            put("/ratings") {
                val averageRatings: Map<String, Float> = call.receive()
                service.updateUserAverageRatings(userId, averageRatings)
                call.respond(HttpStatusCode.OK)
            }
            put("/movies") {
                val movie: Movie = call.receive()
                service.addUserRatedMovie(userId, movie)
                call.respond(HttpStatusCode.OK)
            }
            put("/stats") {
                val stats: UserStats = call.receive()
                service.updateUserStats(userId, stats)
                call.respond(HttpStatusCode.OK)
            }
            delete {
                service.deleteUser(userId)
                call.respond(NoContent)
            }
        }
    }
}
