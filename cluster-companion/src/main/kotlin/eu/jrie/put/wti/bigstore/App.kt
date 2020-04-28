package eu.jrie.put.wti.bigstore

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import eu.jrie.put.wti.bigstore.config.RegistrationClient.registerInHub
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI


@KtorExperimentalAPI
fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            registerModules(KotlinModule(), JavaTimeModule())
        }
    }
}



fun main(args: Array<String>) {
    val (id, cacheHost) = registerInHub().let { (id, cacheHost) ->
        "-P:big-store.companion.id=$id" to "-P:big-store.storage.redis.host=$cacheHost"
    }
    embeddedServer(Netty, commandLineEnvironment(args + id + cacheHost)).start(true)
}