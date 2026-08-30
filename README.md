# Cohors ⚽

Android football companion app built with Kotlin, Jetpack Compose, and Hilt.
Shows league standings, team squads, injuries/suspensions, and tactical lineup visualizations.

## Tech Stack

• Kotlin 2.0.21 + Jetpack Compose (BOM 2024.09.03)
• Hilt 2.52 (dependency injection)
• Retrofit 2.11 + OkHttp 4.12 + Moshi (API-Football v3)
• Room 2.6.1 (offline cache)
• Coil 2.7 (image loading with memory + disk cache)
• Coroutines 1.8.1 + Flow (reactive data)
• MVI architecture (unidirectional data flow)

## Architecture

```
UI (Compose) → ViewModel (StateFlow) → UseCase → Repository → API + Room Cache
```

- **Domain layer**: Use cases, repository interfaces, immutable domain models
- **Data layer**: Retrofit service, DTOs, mappers, Room cache, offline-first repository
- **Presentation layer**: MVI ViewModels with sealed UiState, @Stable screen contracts

## Features

- Browse leagues and teams
- View squad by position (GK → DEF → MID → FWD)
- Injuries & suspensions tab
- Tactical pitch with Canvas-rendered lineup
- Offline-first: cached data served when network unavailable
- Compose stability: @Immutable models, derivedStateOf, Coil cache

## Build

```bash
# Debug
./gradlew assembleDebug

# Release (R8 + resource shrinking)
./gradlew assembleRelease
```

## Configuration

Create `local.properties` with:
```
API_FOOTBALL_KEY=your_rapidapi_key
API_FOOTBALL_HOST=api-football-v1.p.rapidapi.com
```

## Testing

```bash
./gradlew testDebugUnitTest
```
72 unit tests covering mappers, use cases, ViewModels, and repository.

## License

Private project. All rights reserved.
