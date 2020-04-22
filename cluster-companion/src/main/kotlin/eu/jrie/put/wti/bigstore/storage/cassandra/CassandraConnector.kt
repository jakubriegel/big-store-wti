package eu.jrie.put.wti.bigstore.storage.cassandra

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import java.net.InetSocketAddress

@ObsoleteCoroutinesApi
class CassandraConnector (
    host: String
) {
    private val session = CqlSession.builder()
        .addContactPoint(InetSocketAddress(host, 9042))
        .withKeyspace(CqlIdentifier.fromCql("users"))
        .withLocalDatacenter("datacenter1")
        .build()

    private val context = newFixedThreadPoolContext(5, "cassandra-connector")

    suspend fun cql(@Language("CassandraQL") query: String) = withContext(context) {
        session.execute(query)
    } .asFlow()
}
