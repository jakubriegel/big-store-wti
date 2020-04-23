package eu.jrie.put.wti.bigstore.model

import kotlinx.coroutines.sync.Mutex
import java.time.Instant

data class UserCache(
    val user: User,
    val updatedAt: Instant
)

data class UpdateCacheTask (
    val userId: Int
) {
    private val mutex = Mutex(true)

    private lateinit var user: User

    fun complete(result: User) {
        user = result
        mutex.unlock()
    }

    suspend fun await(): User {
        mutex.lock()
        return user
    }
}
