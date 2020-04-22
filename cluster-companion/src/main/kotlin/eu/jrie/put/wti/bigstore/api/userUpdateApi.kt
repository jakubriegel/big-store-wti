package eu.jrie.put.wti.bigstore.api

import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.service.UserUpdateService
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.ObsoleteCoroutinesApi

@ObsoleteCoroutinesApi
fun Application.userUpdateApi() {
    val service = UserUpdateService(CassandraConnector())
    routing {
        route("/user/{id}") {
            put("/ratings") {
                val id: Int = call.parameters["id"]!!.toInt()
                val averageRatings: Map<String, Float> = call.receive()
                service.updateUserAverageRatings(id, averageRatings)
                call.respond(HttpStatusCode.OK)
            }
            put("/movies") {
                val id: Int = call.parameters["id"]!!.toInt()
                val movie: Movie = call.receive()
                service.addUserRatedMovie(id, movie)
                call.respond(HttpStatusCode.OK)
            }
            put("/stats") {
                val id: Int = call.parameters["id"]!!.toInt()
                val stats: UserStats = call.receive()
                service.updateUserStats(id, stats)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}