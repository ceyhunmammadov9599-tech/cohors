package com.cohors.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cohors.app.data.local.entity.*

/**
 * Single DAO for all cache operations.
 * Each entity is upserted by its primary key — stale data is replaced
 * atomically when a fresh API response arrives.
 */
@Dao
interface CacheDao {

    // --- Leagues ---
    @Query("SELECT * FROM league_cache WHERE cacheKey = :key")
    suspend fun getLeagueCache(key: String): LeagueCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLeagueCache(entity: LeagueCacheEntity)

    // --- Teams ---
    @Query("SELECT * FROM team_cache WHERE cacheKey = :key")
    suspend fun getTeamCache(key: String): TeamCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTeamCache(entity: TeamCacheEntity)

    // --- Squad ---
    @Query("SELECT * FROM squad_cache WHERE teamId = :teamId")
    suspend fun getSquadCache(teamId: Int): SquadCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSquadCache(entity: SquadCacheEntity)

    // --- Lineup ---
    @Query("SELECT * FROM lineup_cache WHERE fixtureId = :fixtureId")
    suspend fun getLineupCache(fixtureId: Int): LineupCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLineupCache(entity: LineupCacheEntity)

    // --- Fixture ---
    @Query("SELECT * FROM fixture_cache WHERE teamId = :teamId")
    suspend fun getFixtureCache(teamId: Int): FixtureCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFixtureCache(entity: FixtureCacheEntity)

    // --- Injuries ---
    @Query("SELECT * FROM injury_cache WHERE cacheKey = :key")
    suspend fun getInjuryCache(key: String): InjuryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putInjuryCache(entity: InjuryCacheEntity)
}
