package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import eu.jrie.put.wti.bigstore.storage.redis.RedisConnector
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal class UserCacheServiceTest {

    private val redisMock: RedisConnector = mockk()
    private val cassandraMock: CassandraConnector = mockk()

    private val service = UserCacheService(GlobalScope, redisMock, cassandraMock)

    @Test
    fun `should get user from cache`() = runBlocking {
        // given
//        every { redisMock.get("user-$USER_ID") } returns flowOf(UserCache(mockk(), now()))

        // when
        val result = service.get(USER_ID)

        // then

    }

    @Test
    fun `should fetch user from cache`() {
    }

    @Test
    fun `should get user from db`() {
    }

    @Test
    fun `should get user from cache after timeout`() {
    }

    companion object {
        private const val USER_ID = 1
    }
}