package eu.jrie.put.wti.bigstore

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import eu.jrie.put.wti.bigstore.service.RegistrationService.registerInHub
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.util.KtorExperimentalAPI


@KtorExperimentalAPI
fun Application.main() {

    val (id, cacheHost) = registerInHub(
        environment.config.property("big-store.hub.url").getString(),
        environment.config.property("big-store.storage.redis.hostTemplate").getString()
    )

    install(ContentNegotiation) {
        jackson {
            registerModules(KotlinModule(), JavaTimeModule())
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
