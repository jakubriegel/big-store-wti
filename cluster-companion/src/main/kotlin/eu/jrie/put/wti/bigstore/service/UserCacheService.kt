
package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.model.UpdateCacheTask
import eu.jrie.put.wti.bigstore.model.User
import eu.jrie.put.wti.bigstore.model.UserCache
import eu.jrie.put.wti.bigstore.storage.UserCacheRepository
import eu.jrie.put.wti.bigstore.storage.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant.now
import java.time.temporal.ChronoUnit

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class UserCacheService (
    scope: CoroutineScope,
    private val cache: UserCacheRepository,
    private val repository: UserRepository
) {

    private val updateBuffer = Channel<UpdateCacheTask>(capacity = Channel.UNLIMITED)

    init {
        scope.launch {
            logger.info("started db fetch job")
            updateBuffer.consumeEach { task ->
                fetchUserFromDb(task.userId)
                    .also { task.complete(it) }
                    .also { logger.info("completed fetch task for $it") }
            }
        }
    }

    suspend fun get(id: Int): User = cache.getUser(id)
        .map { getUpToDate(it) }
        .singleOrNull() ?: fetchUserFromDb(id)

    private suspend fun fetchUserFromDb(id: Int) = repository.findUser(id)
        .also { setCache(it) }

    private suspend fun getUpToDate(cached: UserCache) = when {
        cached.isUpToDate() -> {
            logger.info("returning user from cache ${cached.user}")
            cached.user
        }
        else -> tryUpdate(cached.user)
    }

    private suspend fun tryUpdate(user: User) = UpdateCacheTask(user.id).let {
        updateBuffer.send(it)
        withTimeoutOrNull(185) {
            it.await().also { u -> logger.info("returning fresh user from db $u") }
        } ?: user.also { u -> logger.info("returning user from cache after timeout $u") }
    }

    private fun UserCache.isUpToDate() = updatedAt.isAfter(now() - Duration.of(5, ChronoUnit.SECONDS))

    private suspend fun setCache(user: User) {
        cache.setUser(UserCache(user, now()))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserCacheService::class.java)
    }
}
