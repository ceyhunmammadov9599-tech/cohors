package com.cohors.app.di

import com.cohors.app.data.repository.FootballRepositoryImpl
import com.cohors.app.domain.repository.FootballRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the [FootballRepository] domain interface to its concrete
 * [FootballRepositoryImpl] implementation, so ViewModels/UseCases can
 * depend purely on the abstraction.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFootballRepository(
        impl: FootballRepositoryImpl
    ): FootballRepository
}
