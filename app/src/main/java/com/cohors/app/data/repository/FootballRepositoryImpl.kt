package com.cohors.app.data.repository

import android.content.Context
import com.cohors.app.core.util.Resource
import com.cohors.app.core.util.resourceFlow
import com.cohors.app.data.local.dao.CacheDao
import com.cohors.app.data.local.entity.*
import com.cohors.app.data.mapper.toDomain
import com.cohors.app.data.mapper.toDomainList
import com.cohors.app.data.remote.api.ApiFootballService
import com.cohors.app.data.remote.model.*
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.model.TeamLineup
import com.cohors.app.domain.repository.FootballRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [FootballRepository] backed by the
 * API-Football Retrofit service with offline-first Room caching.
 *
 * Strategy: try the network first. On success, cache the response.
 * On failure (no network, timeout, HTTP error), fall back to the
 * Room cache if available, otherwise return the error.
 */
@Singleton
class FootballRepositoryImpl @Inject constructor(
    private val apiService: ApiFootballService,
    private val cacheDao: CacheDao,
    private val moshi: Moshi
) : FootballRepository {

    // --- Moshi list-type adapters for cache serialization ---
    private val leagueListAdapter = moshi.adapter<List<LeagueDto>>(
        Types.newParameterizedType(List::class.java, LeagueDto::class.java)
    )
    private val teamListAdapter = moshi.adapter<List<TeamDto>>(
        Types.newParameterizedType(List::class.java, TeamDto::class.java)
    )
    private val squadAdapter = moshi.adapter(SquadDto::class.java)
    private val squadListAdapter = moshi.adapter<List<SquadDto>>(
        Types.newParameterizedType(List::class.java, SquadDto::class.java)
    )
    private val fixtureListAdapter = moshi.adapter<List<FixtureDto>>(
        Types.newParameterizedType(List::class.java, FixtureDto::class.java)
    )
    private val lineupListAdapter = moshi.adapter<List<LineupDto>>(
        Types.newParameterizedType(List::class.java, LineupDto::class.java)
    )
    private val injuryListAdapter = moshi.adapter<List<InjuryDto>>(
        Types.newParameterizedType(List::class.java, InjuryDto::class.java)
    )

    override fun getLeagues(country: String?, season: Int?): Flow<Resource<List<League>>> =
        cachedResourceFlow(
            cacheKey = "leagues_${country ?: "all"}_${season ?: "all"}",
            getCached = { key -> cacheDao.getLeagueCache(key)?.let { leagueListAdapter.fromJson(it.jsonData)?.toDomainList() } },
            putCached = { key, data -> cacheDao.putLeagueCache(LeagueCacheEntity(key, leagueListAdapter.toJson(data.map { it.toDto() }), System.currentTimeMillis())) },
            fetch = { apiService.getLeagues(country = country, season = season).response?.toDomainList() ?: emptyList() }
        )

    override fun getTeams(leagueId: Int, season: Int): Flow<Resource<List<Team>>> =
        cachedResourceFlow(
            cacheKey = "teams_${leagueId}_$season",
            getCached = { key -> cacheDao.getTeamCache(key)?.let { teamListAdapter.fromJson(it.jsonData)?.toDomainList() } },
            putCached = { key, data -> cacheDao.putTeamCache(TeamCacheEntity(key, teamListAdapter.toJson(data.map { it.toDto() }), System.currentTimeMillis())) },
            fetch = { apiService.getTeams(leagueId = leagueId, season = season).response?.toDomainList() ?: emptyList() }
        )

    override fun getTeamInfo(teamId: Int): Flow<Resource<Team?>> =
        flow {
            emit(Resource.Loading)
            try {
                val response = apiService.getTeamInfo(teamId = teamId)
                val team = response.response?.firstOrNull()?.toDomain()
                emit(Resource.Success(team))
            } catch (e: java.io.IOException) {
                emit(Resource.Error("Bağlantı hatası. İnternetinizi kontrol edin.", e))
            } catch (e: retrofit2.HttpException) {
                emit(Resource.Error(mapHttpError(e.code()), e))
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Beklenmeyen bir hata oluştu.", e))
            }
        }.flowOn(Dispatchers.IO)

    override fun getSquad(teamId: Int): Flow<Resource<List<SquadPlayer>>> =
        cachedResourceFlow(
            cacheKey = teamId.toString(),
            getCached = { key -> cacheDao.getSquadCache(key.toInt())?.let { squadListAdapter.fromJson(it.jsonData)?.firstOrNull()?.players?.toDomainList() } },
            putCached = { key, data ->
                val dto = SquadDto(team = null, players = data.map { it.toDto() })
                cacheDao.putSquadCache(SquadCacheEntity(key.toInt(), squadListAdapter.toJson(listOf(dto)), System.currentTimeMillis()))
            },
            fetch = { apiService.getSquad(teamId = teamId).response?.firstOrNull()?.players?.toDomainList() ?: emptyList() }
        )

    override fun getUpcomingFixture(teamId: Int, season: Int?): Flow<Resource<Fixture?>> =
        flow {
            emit(Resource.Loading)
            try {
                val response = apiService.getUpcomingFixtures(teamId = teamId, next = 1, season = season)
                val fixture = response.response?.firstOrNull()?.toDomain()
                // Cache the fixture
                if (fixture != null) {
                    val dtoList = response.response ?: emptyList()
                    cacheDao.putFixtureCache(FixtureCacheEntity(teamId, fixtureListAdapter.toJson(dtoList), System.currentTimeMillis()))
                }
                emit(Resource.Success(fixture))
            } catch (e: java.io.IOException) {
                // Try cache on failure
                val cached = cacheDao.getFixtureCache(teamId)
                if (cached != null) {
                    val fixture = fixtureListAdapter.fromJson(cached.jsonData)?.firstOrNull()?.toDomain()
                    emit(Resource.Success(fixture))
                } else {
                    emit(Resource.Error("Bağlantı hatası. İnternetinizi kontrol edin.", e))
                }
            } catch (e: retrofit2.HttpException) {
                val cached = cacheDao.getFixtureCache(teamId)
                if (cached != null) {
                    val fixture = fixtureListAdapter.fromJson(cached.jsonData)?.firstOrNull()?.toDomain()
                    emit(Resource.Success(fixture))
                } else {
                    emit(Resource.Error(mapHttpError(e.code()), e))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Beklenmeyen bir hata oluştu.", e))
            }
        }.flowOn(Dispatchers.IO)

    override fun getLineup(fixtureId: Int): Flow<Resource<List<TeamLineup>>> =
        cachedResourceFlow(
            cacheKey = fixtureId.toString(),
            getCached = { key -> cacheDao.getLineupCache(key.toInt())?.let { lineupListAdapter.fromJson(it.jsonData)?.toDomainList() } },
            putCached = { key, data -> cacheDao.putLineupCache(LineupCacheEntity(key.toInt(), lineupListAdapter.toJson(data.map { it.toDto() }), System.currentTimeMillis())) },
            fetch = { apiService.getLineup(fixtureId = fixtureId).response?.toDomainList() ?: emptyList() }
        )

    override fun getInjuries(teamId: Int, season: Int): Flow<Resource<List<Injury>>> =
        cachedResourceFlow(
            cacheKey = "injuries_${teamId}_$season",
            getCached = { key -> cacheDao.getInjuryCache(key)?.let { injuryListAdapter.fromJson(it.jsonData)?.toDomainList() } },
            putCached = { key, data -> cacheDao.putInjuryCache(InjuryCacheEntity(key, injuryListAdapter.toJson(data.map { it.toDto() }), System.currentTimeMillis())) },
            fetch = { apiService.getInjuries(teamId = teamId, season = season).response?.toDomainList() ?: emptyList() }
        )

    // --- Helpers: domain -> DTO for cache serialization ---

    private fun League.toDto(): LeagueDto = LeagueDto(
        league = LeagueInfo(id, name, type, logoUrl),
        country = CountryInfo(countryName, null, countryFlagUrl),
        seasons = currentSeasonYear?.let { listOf(SeasonInfo(it, true)) }
    )

    private fun Team.toDto(): TeamDto = TeamDto(
        team = TeamInfo(id, name, code, country, founded, logoUrl),
        venue = null
    )

    private fun SquadPlayer.toDto(): SquadPlayerDto = SquadPlayerDto(
        id = id, name = name, age = age, number = shirtNumber,
        position = position.name.lowercase().replaceFirstChar { it.uppercase() },
        photo = photoUrl
    )

    private fun TeamLineup.toDto(): LineupDto = LineupDto(
        team = TeamInfo(teamId, teamName, null, null, null, teamLogoUrl),
        formation = formation,
        startXI = startingXI.map { LineupPlayerDto(it.id, it.name, it.shirtNumber, it.positionCode, it.gridPosition) },
        substitutes = substitutes.map { LineupPlayerDto(it.id, it.name, it.shirtNumber, it.positionCode, it.gridPosition) }
    )

    private fun Injury.toDto(): InjuryDto = InjuryDto(
        player = InjuryPlayer(playerId, playerName, playerPhotoUrl),
        team = TeamInfo(null, teamName, null, null, null, null),
        league = FixtureLeague(null, leagueName, null, null),
        fixture = null, type = type, reason = reason
    )

    private fun mapHttpError(code: Int): String = when (code) {
        401, 403 -> "API anahtarı geçersiz veya yetkisiz."
        404 -> "Veri bulunamadı."
        429 -> "Günlük istek limiti aşıldı."
        in 500..599 -> "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
        else -> "İstek başarısız: $code"
    }

    /**
     * Offline-first Flow builder: tries network, caches on success,
     * falls back to Room cache on failure.
     */
    private inline fun <T> cachedResourceFlow(
        cacheKey: String,
        crossinline getCached: suspend (String) -> T?,
        crossinline putCached: suspend (String, T) -> Unit,
        crossinline fetch: suspend () -> T
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading)
        try {
            val data = fetch()
            putCached(cacheKey, data)
            emit(Resource.Success(data))
        } catch (e: java.io.IOException) {
            // Network failure — try cache
            val cached = getCached(cacheKey)
            if (cached != null) {
                emit(Resource.Success(cached))
            } else {
                emit(Resource.Error("Bağlantı hatası. İnternetinizi kontrol edin.", e))
            }
        } catch (e: retrofit2.HttpException) {
            val cached = getCached(cacheKey)
            if (cached != null) {
                emit(Resource.Success(cached))
            } else {
                emit(Resource.Error(mapHttpError(e.code()), e))
            }
        } catch (e: Exception) {
            val cached = getCached(cacheKey)
            if (cached != null) {
                emit(Resource.Success(cached))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Beklenmeyen bir hata oluştu.", e))
            }
        }
    }.flowOn(Dispatchers.IO)
}
