package com.cohors.app.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Generic wrapper for representing the state of a data operation
 * (network/repository call) as it flows through the layers.
 *
 * Usage: repositories emit a stream of Resource<T> via Flow —
 * Loading first, then either Success or Error.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

/**
 * Transforms the payload of a [Resource.Success] while passing
 * Loading/Error through untouched. Use this (instead of a raw `when`)
 * in use cases that reshape repository data for the UI — it sidesteps
 * a Kotlin type-inference quirk where `when` branches mixing
 * Resource<Nothing> and Resource<R> can widen to Resource<Any>.
 */
inline fun <T, R> Resource<T>.mapData(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Loading -> Resource.Loading
    is Resource.Error -> Resource.Error(message, throwable)
    is Resource.Success -> Resource.Success(transform(data))
}

/**
 * Convenience inline helper to build a Loading -> Success/Error Flow<Resource<T>>
 * around a suspend API call, with errors mapped to human-readable messages.
 */
inline fun <T> resourceFlow(
    crossinline block: suspend () -> T
) = flow {
    emit(Resource.Loading)
    try {
        emit(Resource.Success(block()))
    } catch (e: java.io.IOException) {
        emit(Resource.Error(message = "Bağlantı hatası. İnternetinizi kontrol edin.", throwable = e))
    } catch (e: retrofit2.HttpException) {
        val msg = when (e.code()) {
            401, 403 -> "API anahtarı geçersiz veya yetkisiz."
            404 -> "Veri bulunamadı."
            429 -> "Günlük istek limiti aşıldı."
            in 500..599 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
            else -> "İstek başarısız: ${e.code()}"
        }
        emit(Resource.Error(message = msg, throwable = e))
    } catch (e: Exception) {
        emit(Resource.Error(message = e.localizedMessage ?: "Beklenmeyen bir hata oluştu.", throwable = e))
    }
}.flowOn(Dispatchers.IO)
