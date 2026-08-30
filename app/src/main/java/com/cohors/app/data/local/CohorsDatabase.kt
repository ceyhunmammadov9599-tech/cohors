package com.cohors.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cohors.app.data.local.dao.CacheDao
import com.cohors.app.data.local.entity.*

/**
 * Room database for offline-first caching.
 * Stores API responses as JSON strings to keep the schema simple
 * while enabling instant offline reads.
 */
@Database(
    entities = [
        LeagueCacheEntity::class,
        TeamCacheEntity::class,
        SquadCacheEntity::class,
        LineupCacheEntity::class,
        FixtureCacheEntity::class,
        InjuryCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CohorsDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
