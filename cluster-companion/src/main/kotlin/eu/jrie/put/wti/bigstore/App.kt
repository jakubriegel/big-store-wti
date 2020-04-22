package eu.jrie.put.wti.bigstore

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            registerModules(KotlinModule(), JavaTimeModule())
        }
    }
}

fun main(args: Array<String>) {
    runBlocking { delay(5_000) } // wait for Cassandra TODO: fix
    io.ktor.server.netty.EngineMain.main(args)
}
