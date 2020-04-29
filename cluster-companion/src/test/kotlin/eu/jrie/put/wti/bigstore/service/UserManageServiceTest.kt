package eu.jrie.put.wti.bigstore.service

import eu.jrie.put.wti.bigstore.model.Movie
import eu.jrie.put.wti.bigstore.model.UserStats
import eu.jrie.put.wti.bigstore.storage.cassandra.CassandraConnector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant.now

@ObsoleteCoroutinesApi
internal class UserManageServiceTest {

    private val cassandraMock: CassandraConnector = mockk()
    private val service = UserManageService(cassandraMock)

    private val userId = 1

    @BeforeEach
    fun before() {
        coEvery { cassandraMock.cql(any()) } returns emptyFlow()
    }

    @Test
    fun `should update user average ratings`() = runBlocking {
        // given
        val ratings = mapOf("comedy" to 5.6f, "action" to 7.8f)

        // when
        service.updateUserAverageRatings(userId, ratings)

        // then
        val desiredCql = "UPDATE user_avg SET avg_comedy = 5.6, avg_action = 7.8 WHERE user_id = $userId"
        coVerify(exactly = 1) { cassandraMock.cql(desiredCql) }
    }

    @Test
    fun `should add new user rated movie`() = runBlocking {
        // given
        val movie = Movie(1, "action", 1.2f)

        // when
        service.addUserRatedMovie(userId, movie)

        // then
        val desiredCql = "INSERT INTO user_rated_movies (user_id, movie_id, genre, rating) VALUES ($userId, ${movie.id}, '${movie.genre}', ${movie.rating})"
        coVerify(exactly = 1) { cassandraMock.cql(desiredCql) }
    }

    @Test
    fun `should update user stats`() = runBlocking {
        // given
        val stats = UserStats(now())

        // when
        service.updateUserStats(userId, stats)

        // then
        val desiredCql = "UPDATE user_stats SET last_active = ${stats.lastActive.toEpochMilli()} WHERE user_id = $userId"
        coVerify(exactly = 1) { cassandraMock.cql(desiredCql) }
    }

    @Test
    fun `should delete all user data`() = runBlocking {
        // when
        service.deleteUser(userId)

        // then
        coVerify(exactly = 1) {
            cassandraMock.cql("DELETE FROM user_avg WHERE user_id = $userId")
            cassandraMock.cql("DELETE FROM user_rated_movies WHERE user_id = $userId")
            cassandraMock.cql("DELETE FROM user_stats WHERE user_id = $userId")
        }
    }
}
