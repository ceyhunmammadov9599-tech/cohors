package com.cohors.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room cache entities for offline-first data access.
 * Each entity stores the API response payload as a JSON string
 * to avoid excessive schema complexity while enabling instant
 * offline reads with Moshi deserialization.
 */

@Entity(tableName = "league_cache")
data class LeagueCacheEntity(
    @PrimaryKey val cacheKey: String,    // "all" or "country_<name>" or "season_<year>"
    val jsonData: String,                // Serialized List<LeagueDto>
    val cachedAt: Long                   // System.currentTimeMillis()
)

@Entity(tableName = "team_cache")
data class TeamCacheEntity(
    @PrimaryKey val cacheKey: String,    // "league_<id>_season_<year>"
    val jsonData: String,                // Serialized List<TeamDto>
    val cachedAt: Long
)

@Entity(tableName = "squad_cache")
data class SquadCacheEntity(
    @PrimaryKey val teamId: Int,
    val jsonData: String,                // Serialized SquadDto
    val cachedAt: Long
)

@Entity(tableName = "lineup_cache")
data class LineupCacheEntity(
    @PrimaryKey val fixtureId: Int,
    val jsonData: String,                // Serialized List<LineupDto>
    val cachedAt: Long
)

@Entity(tableName = "fixture_cache")
data class FixtureCacheEntity(
    @PrimaryKey val teamId: Int,
    val jsonData: String,                // Serialized FixtureDto
    val cachedAt: Long
)

@Entity(tableName = "injury_cache")
data class InjuryCacheEntity(
    @PrimaryKey val cacheKey: String,    // "team_<id>_season_<year>"
    val jsonData: String,                // Serialized List<InjuryDto>
    val cachedAt: Long
)
