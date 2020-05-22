package eu.jrie.put.wti.bigstore.model

import java.time.Instant

data class User (
    val id: Int,
    val averageRating: Map<String, Float>,
    val ratedMovies: List<Movie>,
    val stats: UserStats
)

data class Movie (
    val id: Int,
    val genre: List<String>,
    val rating: Float
)

data class UserStats (
    val lastActive: Instant
)
