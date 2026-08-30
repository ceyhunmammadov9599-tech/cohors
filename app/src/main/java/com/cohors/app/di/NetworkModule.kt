package com.cohors.app.di

import android.content.Context
import com.cohors.app.BuildConfig
import com.cohors.app.data.remote.api.ApiFootballService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api-football-v1.p.rapidapi.com/v3/"
    private const val CACHE_SIZE_BYTES: Long = 10 * 1024 * 1024 // 10 MB

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    @Named("AuthInterceptor")
    fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", BuildConfig.API_FOOTBALL_KEY)
            .addHeader("X-RapidAPI-Host", BuildConfig.API_FOOTBALL_HOST)
            .build()
        chain.proceed(request)
    }

    /**
     * Offline cache interceptor: serves cached responses when no network
     * connection is available. Online requests get max-stale=5min so
     * repeated calls within 5 minutes use the cache.
     */
    @Provides
    @Singleton
    @Named("OfflineInterceptor")
    fun provideOfflineInterceptor(@ApplicationContext context: Context): Interceptor =
        Interceptor { chain ->
            var request = chain.request()
            val isOnline = try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val network = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(network)
                caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (e: Exception) {
                false
            }
            if (!isOnline) {
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=${60 * 60 * 24 * 7}")
                    .build()
            }
            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "cohors_http_cache"), CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named("AuthInterceptor") authInterceptor: Interceptor,
        @Named("OfflineInterceptor") offlineInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        cache: Cache
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(offlineInterceptor)
        .addInterceptor(loggingInterceptor)
        .cache(cache)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideApiFootballService(retrofit: Retrofit): ApiFootballService =
        retrofit.create(ApiFootballService::class.java)
}
