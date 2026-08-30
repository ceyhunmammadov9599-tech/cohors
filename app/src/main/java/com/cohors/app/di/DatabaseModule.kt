package com.cohors.app.di

import android.content.Context
import androidx.room.Room
import com.cohors.app.data.local.CohorsDatabase
import com.cohors.app.data.local.dao.CacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CohorsDatabase =
        Room.databaseBuilder(
            context,
            CohorsDatabase::class.java,
            "cohors.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCacheDao(database: CohorsDatabase): CacheDao =
        database.cacheDao()
}
