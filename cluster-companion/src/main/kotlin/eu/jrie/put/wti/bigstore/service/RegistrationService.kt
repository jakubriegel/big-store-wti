package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.storage.redis.RedisConnector
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetAddress

object RegistrationService {

    private data class RegistrationResponse(val id: Int)

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
        }
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        defaultRequest {
            header("Companion-Host", InetAddress.getLocalHost().hostAddress)
        }
    }

    fun registerInHub(hubUrl: String, cacheHostTemplate: String): Pair<Int, String> = runBlocking {
        client.post<RegistrationResponse>("$hubUrl/register")
            .id
            .let { it to String.format(cacheHostTemplate, it) }
            .also { (id, cacheHost) -> logger.info("Registered with id $id and cacheHost $cacheHost") }
            .let { (id, cacheHost) -> testRedis(id, cacheHost, hubUrl) }
    } ?: throw RuntimeException("Cannot register in hub")

    private suspend fun testRedis(id: Int, cacheHost: String, hubUrl: String) = RedisConnector.test(cacheHost)
        .let { connectedToRedis ->
            when {
                connectedToRedis -> {
                    logger.info("Successfully tested connection with Redis")
                    registerAsReady(id, cacheHost, hubUrl)
                }
                else -> {
                    logger.info("Cannot connect to redis")
                    null
                }
            }
        }

    private suspend fun registerAsReady(id: Int, cacheHost: String, hubUrl: String) =
        client.post<HttpResponse>("$hubUrl/register/$id/ready")
            .let {
                when (it.status) {
                    HttpStatusCode.OK -> {
                        logger.info("Successfully registered as ready")
                        id to cacheHost
                    }
                    else -> {
                        logger.info("Cannot register as ready, status: ${it.status}")
                        null
                    }
                }
            }

    private val logger = LoggerFactory.getLogger(RegistrationService::class.java)
}