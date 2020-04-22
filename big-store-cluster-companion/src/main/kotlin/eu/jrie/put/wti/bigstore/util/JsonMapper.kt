package eu.jrie.put.wti.bigstore.util

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

@ObsoleteCoroutinesApi
class JsonMapper {
    private val context = newSingleThreadContext("JsonMapperContext")

    private val mapper = jacksonObjectMapper().apply {
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
    }

    suspend inline fun <reified T : Any> read(json: String) = read(json, T::class)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun <T : Any> read(json: String, type: KClass<T>): T = withContext(context) {
        mapper.readValue(json, type.java)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun write(entity: Any) = withContext(context) {
        mapper.writeValueAsString(entity)
    }
}
