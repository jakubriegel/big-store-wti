package eu.jrie.put.wti.bigstore.storage

import eu.jrie.put.wti.bigstore.model.UserCache
import eu.jrie.put.wti.bigstore.storage.redis.RedisConnector
import eu.jrie.put.wti.bigstore.util.JsonMapper
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ObsoleteCoroutinesApi
class UserCacheRepository (
    private val redis: RedisConnector,
    private val mapper: JsonMapper,
    private val ttl: Long
) {
    suspend fun getUser(id: Int): Flow<UserCache> = redis.get(userKey(id))
        .map { mapper.read(it) as UserCache }

    suspend fun setUser(value: UserCache) {
        redis.apply {
            set(value.key(), mapper.write(value))
            expire(value.key(), ttl)
        }
    }

    companion object {
        private fun UserCache.key() = userKey(user.id)
        private fun userKey(id: Int) = "user-$id"
    }
}