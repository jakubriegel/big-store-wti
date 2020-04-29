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
import java.lang.System.getenv


@KtorExperimentalAPI
fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            registerModules(KotlinModule(), JavaTimeModule())
        }
    }
}

private enum class Env {
    PROD, DEV
}

fun main(args: Array<String>) = startCompanion(args, Env.valueOf(getenv("BS_ENV") ?: "DEV"))

private fun startCompanion(args: Array<String>, env: Env) {
    when (env) {
        Env.PROD -> registerInHub().let { (id, cacheHost) ->
            args + "-P:big-store.companion.id=$id" + "-P:big-store.cache.redis.host=$cacheHost"
        }
        Env.DEV -> args
    }.let {
        embeddedServer(Netty, commandLineEnvironment(it)).start(true)
    }
}
