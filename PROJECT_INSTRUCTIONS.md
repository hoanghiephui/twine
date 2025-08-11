# Twine RSS Reader - Project Instructions

## Project Overview

**Twine** is a cross-platform RSS reader application built with Kotlin Multiplatform and Compose Multiplatform.

## Project Structure

```
twine/
├── androidApp/                     # Android application target
│   ├── src/
│   │   ├── main/
│   │   └── debug/
│   └── build.gradle.kts
├── iosApp/                        # iOS application wrapper
│   ├── iosApp/
│   │   ├── ContentView.swift
│   │   ├── iOSApp.swift
│   │   └── Info.plist
│   └── iosApp.xcodeproj/
├── shared/                        # Shared multiplatform code
│   ├── src/
│   │   ├── commonMain/           # Shared business logic & UI
│   │   ├── androidMain/          # Android-specific implementations
│   │   ├── iosMain/              # iOS-specific implementations
│   │   └── commonTest/           # Shared tests
│   └── build.gradle.kts
├── core/                         # Core modules
│   ├── base/                     # Base utilities & contracts
│   ├── model/                    # Data models (local & remote)
│   ├── data/                     # Data layer (repositories)
│   └── network/                  # Network layer
├── resources/                    # Resource modules
│   └── icons/                    # Icon resources
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── build.gradle.kts              # Root build configuration
└── settings.gradle.kts           # Project settings
```

## Application Architecture

### 1. Clean Architecture Layers

#### **Presentation Layer** (`shared/src/commonMain/kotlin/.../ui/`)
- **Compose UI Screens**: HomeScreen, ReaderScreen, SearchScreen, SettingsScreen, etc.
- **Presenters**: Business logic for UI (HomePresenter, BookmarksPresenter, etc.)
- **Navigation**: Decompose navigation with stack-based routing
- **State Management**: StateFlow and Compose state

#### **Domain Layer** (`core/model/`)
- **Entities**: Feed, Post, FeedGroup, PostWithMetadata
- **Use Cases**: Implemented through Presenters
- **Repository Interfaces**: Define contracts for data access

#### **Data Layer** (`core/data/`)
- **Repositories**: RssRepository, SettingsRepository
- **Data Sources**: Local (SQLDelight) and Remote (Ktor)
- **Database**: SQLDelight with platform-specific drivers

#### **Infrastructure Layer** (`core/network/`)
- **Network Client**: Ktor HTTP client
- **Feed Parsers**: RSS, Atom, RDF, JSON feed parsers
- **API Services**: HTTP services for feed fetching

### 2. Dependency Injection

Uses **kotlin-inject** with component-based architecture:

```kotlin
@AppScope
@Component
abstract class ApplicationComponent : SharedApplicationComponent() {
  // Platform-specific dependencies
}

interface SharedApplicationComponent : 
  DataComponent, 
  NetworkComponent, 
  LoggingComponent {
  // Shared dependencies
}
```

### 3. Platform Abstractions

#### Android-specific (`shared/src/androidMain/`, `core/*/src/androidMain/`)
- `ApplicationComponent.kt`: Android DI setup
- `DriverFactory.kt`: SQLite driver for Android
- `NetworkComponent.kt`: OkHttp engine

#### iOS-specific (`shared/src/iosMain/`, `core/*/src/iosMain/`)
- `ApplicationComponent.kt`: iOS DI setup  
- `DriverFactory.kt`: Native SQLite driver for iOS
- `NetworkComponent.kt`: Darwin engine
- `HomeViewController.kt`: Bridge to integrate Compose with SwiftUI

## Detailed Tech Stack

### Core Technologies
- **Kotlin Multiplatform 2.2.0**: Shared business logic
- **Compose Multiplatform 1.9.0**: Cross-platform UI framework
- **Decompose**: Navigation and lifecycle management
- **kotlin-inject 0.8.0**: Compile-time dependency injection

### Data & Persistence
- **SQLDelight 2.1.0**: SQL database with type-safe queries
- **DataStore 1.1.7**: Key-value preferences storage
- **Paging 3.3.0**: Pagination support

### Network & Parsing  
- **Ktor 3.2.2**: HTTP client with caching support
- **kotlinx.serialization**: JSON serialization
- **XML parsing**: Support for RSS/Atom/RDF feeds
- **HTML parsing**: KSoup for content parsing

### UI & Theming
- **Material 3 Design**: Modern Material Design
- **Dynamic Theming**: Content-based color theming
- **Markdown Rendering**: Rich text support for articles
- **Image Loading**: Coil3 for async image loading with optimizations

### Development Tools
- **Spotless + ktfmt**: Code formatting
- **KSP**: Annotation processing
- **Bugsnag**: Error tracking and crash reporting
- **Kermit**: Multiplatform logging

### Image Loading Optimizations
- **ImagePreloader**: Smart preloading with concurrency control
- **Enhanced Caching**: 30% memory cache, 8% disk cache
- **Dynamic Preloading**: Load next 10 items during scroll
- **Crossfade Animations**: Smooth image transitions
- **Dedicated FavIcon Cache**: Separate cache pool for feed icons

For detailed information, see [IMAGE_LOADING_OPTIMIZATIONS.md](IMAGE_LOADING_OPTIMIZATIONS.md)

## Patterns & Best Practices

### 1. Presenter Pattern (MVP-like)
```kotlin
@Inject
class HomePresenter(
  private val rssRepository: RssRepository,
  @Assisted componentContext: ComponentContext,
  @Assisted private val onPostClick: (PostWithMetadata) -> Unit
) : ComponentContext by componentContext {
  
  internal val state: StateFlow<HomeState> = /* ... */
  
  fun dispatch(event: HomeEvent) {
    // Handle UI events
  }
}
```

### 2. Repository Pattern
```kotlin
@Inject
@AppScope
class RssRepository(
  private val feedQueries: FeedQueries,
  private val postQueries: PostQueries,
  private val feedFetcher: FeedFetcher
) {
  suspend fun addFeed(feedLink: String): Result<Feed>
  fun feeds(): Flow<List<Feed>>
  // ... other methods
}
```

### 3. Multiplatform Expect/Actual
```kotlin
// commonMain
expect class DriverFactory {
  fun createDriver(): SqlDriver
}

// androidMain  
actual class DriverFactory(context: Context) {
  actual fun createDriver(): SqlDriver = AndroidSqliteDriver(...)
}

// iosMain
actual class DriverFactory() {
  actual fun createDriver(): SqlDriver = NativeSqliteDriver(...)
}
```

### 4. Flow-based Reactive Programming
```kotlin
val postsFlow = feedQueries.allPosts()
  .asFlow()
  .mapToList(dispatchersProvider.io)
  .map { posts -> posts.map { it.toModel() } }
  .flowOn(dispatchersProvider.io)
```

### 5. Paging Support
```kotlin
val bookmarks = createPager(
  config = createPagingConfig(pageSize = 20)
) { 
  rssRepository.bookmarks() 
}.flow.cachedIn(coroutineScope)
```

## Development Workflow

### 1. Environment Setup
```bash
# Requirements
- JDK 20+
- Android Studio (latest stable)
- Xcode (for iOS development)

# Clone and build
git clone <repository>
cd twine
./gradlew build
```

### 2. Code Style
```bash
# Format code before commit
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### 3. Testing
```bash
# Run tests
./gradlew test

# Android tests
./gradlew connectedAndroidTest

# iOS tests (via Xcode)
xcodebuild test -scheme iosApp
```

### 4. Build & Release
```bash
# Android debug build
./gradlew :androidApp:assembleDebug

# Android release build  
./gradlew :androidApp:assembleRelease

# iOS build (via Xcode)
# Open iosApp/iosApp.xcodeproj
```

## Implementation Guidelines

### 1. Adding New Features

#### Step 1: Create Models (if needed)
```kotlin
// core/model/src/commonMain/kotlin/.../model/local/
data class NewFeature(
  val id: String,
  val name: String,
  // ... other properties
)
```

#### Step 2: Database Schema (if needed)
```sql
-- core/data/src/commonMain/sqldelight/.../database/
CREATE TABLE new_feature (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL
);
```

#### Step 3: Repository Methods
```kotlin
// core/data/src/commonMain/kotlin/.../repository/
suspend fun addNewFeature(feature: NewFeature)
fun newFeatures(): Flow<List<NewFeature>>
```

#### Step 4: Presenter
```kotlin
// shared/src/commonMain/kotlin/.../newfeature/
@Inject
class NewFeaturePresenter(
  private val repository: RssRepository,
  @Assisted componentContext: ComponentContext
) : ComponentContext by componentContext
```

#### Step 5: UI Screen
```kotlin
// shared/src/commonMain/kotlin/.../newfeature/ui/
@Composable
fun NewFeatureScreen(presenter: NewFeaturePresenter) {
  // Compose UI implementation
}
```

#### Step 6: Navigation Integration
```kotlin
// shared/src/commonMain/kotlin/.../app/AppPresenter.kt
Config.NewFeature -> {
  Screen.NewFeature(
    presenter = newFeaturePresenter(componentContext) { navigation.pop() }
  )
}
```

### 2. Platform-specific Implementation

#### When platform-specific code is needed:
```kotlin
// commonMain
expect fun platformSpecificFunction(): String

// androidMain
actual fun platformSpecificFunction(): String = "Android Implementation"

// iosMain  
actual fun platformSpecificFunction(): String = "iOS Implementation"
```

### 3. Network Integration

#### Adding new API:
```kotlin
// core/network/src/commonMain/kotlin/.../
class ApiService(private val httpClient: HttpClient) {
  suspend fun fetchData(): Result<DataResponse> {
    return try {
      val response = httpClient.get("https://api.example.com/data")
      Result.success(response.body())
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
```

### 4. Database Migration

#### When changing schema:
```kotlin
// core/data/src/commonMain/kotlin/.../migrations/
val migration_X_to_Y = AfterVersion(X) { driver ->
  driver.execute(null, "ALTER TABLE ...", 0)
}
```

## Debugging & Troubleshooting

### 1. Common Issues

#### Dependency Injection Issues
- Check `@Inject` annotations
- Verify component hierarchy
- Check scope annotations (`@AppScope`, `@ActivityScope`)

#### Database Issues  
- Check migration scripts
- Verify foreign key constraints
- Check SQLDelight generated code

#### Navigation Issues
- Verify `Config` serialization
- Check component context passing
- Validate navigation stack state

### 2. Logging
```kotlin
// Use Kermit for logging
Logger.d("ClassName") { "Debug message" }
Logger.e("ClassName", throwable) { "Error message" }
```

### 3. Performance Monitoring
- Use Bugsnag for crash reporting
- Monitor database query performance
- Check memory usage with LeakCanary (Android)

## Contributing Guidelines

### 1. Code Review Checklist
- [ ] Code follows ktfmt formatting
- [ ] Tests added for new functionality  
- [ ] Documentation updated
- [ ] No breaking changes without migration
- [ ] Performance impact considered
- [ ] Accessibility support maintained

### 2. Pull Request Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature  
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code formatted with spotless
- [ ] No new warnings introduced
- [ ] Documentation updated
```

### 3. Release Process
1. Update version in `gradle.properties`
2. Update `CHANGELOG.md`
3. Run full test suite
4. Create release tag
5. Build and deploy to stores

## Useful Commands

```bash
# Development
./gradlew spotlessApply              # Format code
./gradlew build                      # Full build
./gradlew :shared:testDebugUnitTest  # Run shared tests

# Android
./gradlew :androidApp:assembleDebug  # Build Android APK
./gradlew :androidApp:installDebug   # Install on device

# iOS (via command line)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp build

# Clean
./gradlew clean                      # Clean all modules
./gradlew :shared:clean              # Clean specific module
```

---

**Note**: This project uses cutting-edge technologies and modern patterns. Please refer to the official documentation of each library to better understand usage and best practices.
