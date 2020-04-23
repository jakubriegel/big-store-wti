package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.model.User
import eu.jrie.put.wti.bigstore.model.UserCache
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.UserCacheRepository
import eu.jrie.put.wti.bigstore.storage.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.Instant.EPOCH
import java.time.Instant.now

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal class UserCacheServiceTest {

    private val cacheMock: UserCacheRepository = mockk()
    private val repositoryMock: UserRepository = mockk()

    private val service = UserCacheService(GlobalScope, cacheMock, repositoryMock)

    @Test
    fun `should get fresh user from cache`() = runBlocking {
        // given
        val storedCache = userCache()
        coEvery { cacheMock.getUser(USER_ID) } returns flowOf(storedCache)

        // when
        val result = service.get(USER_ID)

        // then
        coVerify { cacheMock.getUser(USER_ID) }
        assertEquals(storedCache.user, result)
    }

    @Test
    fun `should get unknown user from db and update cache`() = runBlocking {
        // given
        coEvery { cacheMock.getUser(USER_ID) } returns emptyFlow()
        coEvery { cacheMock.setUser(any()) } just runs
        coEvery { repositoryMock.findUser(USER_ID) } returns userCache().user

        // when
        val result = service.get(USER_ID)

        // then
        coVerifySequence {
            cacheMock.getUser(USER_ID)
            repositoryMock.findUser(USER_ID)
            cacheMock.setUser(match { it.user.id == USER_ID })
        }
        assertEquals(USER_ID, result.id)
    }

    @Test
    fun `should get expired user from db and update cache`() = runBlocking {
        // given
        val storedCache = userCache(EPOCH)
        coEvery { cacheMock.getUser(USER_ID) } returns flowOf(storedCache)
        coEvery { cacheMock.setUser(any()) } just runs
        coEvery { repositoryMock.findUser(USER_ID) } returns userCache().user

        // when
        val result = service.get(USER_ID)

        // then
        coVerifySequence {
            cacheMock.getUser(USER_ID)
            repositoryMock.findUser(USER_ID)
            cacheMock.setUser(match { (it.user.id == USER_ID) and (it.updatedAt > storedCache.updatedAt) })
        }
        assertNotEquals(storedCache.user, result)
        assertEquals(storedCache.user.id, result.id)
    }

    @Test
    fun `should get expired user from cache after timeout`() = runBlocking {
        // given
        val storedCache = userCache(EPOCH)
        coEvery { cacheMock.getUser(USER_ID) } returns flowOf(storedCache)
        coEvery { cacheMock.setUser(any()) } just runs
        coEvery { repositoryMock.findUser(USER_ID) } coAnswers { delay(1000); userCache().user }

        // when
        val result = service.get(USER_ID)

        // then
        delay(1500)
        coVerifySequence {
            cacheMock.getUser(USER_ID)
            repositoryMock.findUser(USER_ID)
            cacheMock.setUser(match { (it.user.id == USER_ID) and (it.updatedAt > storedCache.updatedAt) })
        }
        assertEquals(storedCache.user, result)
    }

    companion object {
        private const val USER_ID = 1
        private fun userCache(updatedAt: Instant = now()) = UserCache(
            User(USER_ID, emptyMap(), emptyList(), UserStats(updatedAt)),
            updatedAt
        )
    }
}