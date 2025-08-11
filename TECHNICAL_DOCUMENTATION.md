# Twine RSS Reader - Technical Documentation

## Architecture Overview

**Twine** implements a sophisticated multiplatform architecture using Kotlin Multiplatform and Compose Multiplatform. The project follows Clean Architecture principles with clear separation of concerns across multiple layers.

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
├─────────────────┬─────────────────┬─────────────────────────┤
│   Android UI    │   Shared UI     │      iOS UI             │
│   MainActivity  │ Compose Screens │   SwiftUI Bridge        │
│   AppModule     │   Presenters    │   ContentView           │
└─────────────────┴─────────────────┴─────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
├─────────────────────────────────────────────────────────────┤
│     Business Logic     │    Models    │   Use Cases         │
│     Presenters         │   Entities   │   (via Presenters)  │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                              │
├─────────────────┬─────────────────┬─────────────────────────┤
│   Repositories  │  Local Storage  │   Remote Data Source    │
│  RssRepository  │   SQLDelight    │     Ktor Client         │
│ SettingsRepo    │   DataStore     │   Feed Parsers          │
└─────────────────┴─────────────────┴─────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                Infrastructure Layer                         │
├─────────────────┬─────────────────┬─────────────────────────┤
│  Android Impl   │   Common Impl   │     iOS Impl            │
│  OkHttp Driver  │  Shared Logic   │   Darwin Engine         │
│  Android SQLite │   Abstractions  │   iOS SQLite            │
└─────────────────┴─────────────────┴─────────────────────────┘
```

## Module Dependencies

### Core Modules Structure

```
core/
├── base/                   # Core abstractions & utilities
│   ├── DispatchersProvider # Coroutine dispatchers abstraction
│   ├── AppScope           # DI scopes definition
│   └── Platform utilities
├── model/                  # Data models & contracts
│   ├── local/             # Local domain models
│   │   ├── Feed.kt
│   │   ├── Post.kt
│   │   ├── PostWithMetadata.kt
│   │   └── FeedGroup.kt
│   └── remote/            # Remote/API models
│       ├── FeedPayload.kt
│       └── PostPayload.kt
├── data/                  # Data layer implementation
│   ├── repository/        # Repository implementations
│   ├── database/          # SQLDelight database
│   └── di/               # Data layer DI
└── network/              # Network layer
    ├── fetcher/          # Feed fetching logic
    ├── parser/           # Feed format parsers
    └── di/              # Network DI
```

### Shared Module Structure

```
shared/src/
├── commonMain/kotlin/
│   ├── dev/sasikanth/rss/reader/
│   │   ├── app/                    # Application coordinator
│   │   ├── home/                   # Home screen
│   │   ├── reader/                 # Article reader
│   │   ├── search/                 # Search functionality
│   │   ├── bookmarks/              # Bookmarks management
│   │   ├── settings/               # App settings
│   │   ├── feeds/                  # Feed management
│   │   ├── about/                  # About screen
│   │   ├── components/             # Shared UI components
│   │   ├── di/                     # Dependency injection
│   │   └── platform/               # Platform abstractions
├── androidMain/kotlin/             # Android-specific implementations
└── iosMain/kotlin/                 # iOS-specific implementations
```

## Data Flow Architecture

### State Management Flow

```
User Interaction → UI Event → Presenter → Repository → Data Source
                                ↓
UI Update ← State Flow ← Business Logic ← Transformed Data
```

### Example: Feed Loading Flow

```kotlin
// 1. User triggers refresh
HomeScreen -> HomePresenter.dispatch(HomeEvent.Refresh)

// 2. Presenter handles event
HomePresenter -> RssRepository.refreshFeeds()

// 3. Repository coordinates data fetching
RssRepository -> FeedFetcher.fetchAllFeeds()
               -> FeedQueries.updateFeeds()

// 4. Database triggers reactive update
SQLDelight -> Flow<List<Feed>> -> HomePresenter.state

// 5. UI recomposes automatically
StateFlow -> @Composable HomeScreen recomposition
```

## Database Design

### Database Initialization Flow

Ứng dụng Twine khởi tạo database theo quy trình sau:

#### 1. Application Launch
```kotlin
// Android: ReaderApplication.onCreate()
// iOS: AppDelegate.didFinishLaunchingWithOptions()
override fun onCreate() {
  super.onCreate()
  
  // Initialize all components including database
  appComponent.initializers.forEach { it.initialize() }
}
```

#### 2. Database Creation Process

**Platform-specific Database Driver Factory**:
```kotlin
// Android Implementation
@Inject
@AppScope
actual class DriverFactory(
  private val context: Context,
  private val codeMigrations: Array<AfterVersion>,
  private val prePopulateFeedQueries: Array<String>,
) {
  actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(
      schema = ReaderDatabase.Schema,
      context = context,
      name = DB_NAME, // "rss_reader.db"
      callback = object : AndroidSqliteDriver.Callback(
        ReaderDatabase.Schema, 
        callbacks = codeMigrations
      ) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          super.onCreate(db) // Creates all tables using SQLDelight schema
          // Pre-populate with default feeds
          prePopulateFeedQueries.forEach { query -> db.execSQL(query) }
        }
        
        override fun onConfigure(db: SupportSQLiteDatabase) {
          super.onConfigure(db)
          db.enableWriteAheadLogging() // Performance optimization
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
          db.setForeignKeyConstraintsEnabled(true) // Enable FK constraints
        }
      }
    )
  }
}

// iOS Implementation  
@Inject
@AppScope
actual class DriverFactory(
  private val codeMigrations: Array<AfterVersion>,
  private val prePopulateFeedQueries: Array<String>,
) {
  actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(
      DatabaseConfiguration(
        name = DB_NAME,
        version = ReaderDatabase.Schema.version.toInt(),
        journalMode = JournalMode.WAL, // Write-Ahead Logging
        create = { connection ->
          wrapConnection(connection) { ReaderDatabase.Schema.create(it) }
          // Pre-populate with default feeds
          prePopulateFeedQueries.forEach { query -> connection.rawExecSql(query) }
        },
        upgrade = { connection, oldVersion, newVersion ->
          wrapConnection(connection) {
            ReaderDatabase.Schema.migrate(
              driver = it,
              oldVersion = oldVersion.toLong(),
              newVersion = newVersion.toLong(),
              callbacks = codeMigrations
            )
          }
        }
      )
    )
  }
}
```

#### 3. Pre-populated Default Feeds

Khi database được tạo lần đầu, ứng dụng tự động thêm 4 feeds mặc định:

```kotlin
@Provides
@AppScope
fun providesPrePopulateFeedQueries(): Array<String> {
  return arrayOf(
    // Kottke.org
    """
      INSERT OR IGNORE INTO feed(id, name, icon, description, link, homepageLink, createdAt, pinnedAt)
      VALUES (
          'ba2ba021-2f69-55ad-9c21-cdf1a555e9bf',
          'Kottke',
          "Jason Kottke's weblog, home of fine hypertext products since 1998",
          'https://icon.horse/icon/kottke.org',
          'https://feeds.kottke.org/main',
          'https://kottke.org/',
          (strftime('%s', 'now') * 1000),
          (strftime('%s', 'now') * 1000)
      );
    """.trimIndent(),
    
    // Hacker News
    """
      INSERT OR IGNORE INTO feed(id, name, icon, description, link, homepageLink, createdAt, pinnedAt)
      VALUES (
          'c90003bd-b1e6-5545-ba59-3d2128d658a7',
          'HN',
          'Links for the intellectually curious, ranked by readers.',
          'https://icon.horse/icon/news.ycombinator.com',
          'https://news.ycombinator.com/rss',
          'https://news.ycombinator.com/',
          (strftime('%s', 'now') * 1000),
          (strftime('%s', 'now') * 1000)
      );
    """.trimIndent(),
    
    // The Verge
    """
      INSERT OR IGNORE INTO feed(id, name, icon, description, link, homepageLink, createdAt, pinnedAt)
      VALUES (
          'e8d31cec-2893-54d0-bcae-7f134713e532',
          'The Verge',
          'The Verge is about technology and how it makes us feel...',
          'https://platform.theverge.com/wp-content/uploads/sites/2/2025/01/verge-rss-large_80b47e.png',
          'https://www.theverge.com/rss/index.xml',
          'https://www.theverge.com',
          (strftime('%s', 'now') * 1000),
          (strftime('%s', 'now') * 1000)
      );
    """.trimIndent(),
    
    // New York Times World News
    """
      INSERT OR IGNORE INTO feed(id, name, icon, description, link, homepageLink, createdAt, pinnedAt)
      VALUES (
          '9ef86906-12bd-573a-bc19-ca1f2381793a',
          'NYT > World News',
          'New York Times world news',
          'https://static01.nyt.com/images/misc/NYT_logo_rss_250x40.png',
          'https://rss.nytimes.com/services/xml/rss/nyt/World.xml',
          'https://www.nytimes.com/section/world',
          (strftime('%s', 'now') * 1000),
          (strftime('%s', 'now') * 1000)
      );
    """.trimIndent()
  )
}
```

#### 4. Database Migration System

Ứng dụng sử dụng SQLDelight migrations cho schema updates:

```kotlin
object SQLCodeMigrations {
  fun migrations(): Array<AfterVersion> {
    return arrayOf(
      afterVersion12(), // Migrate post IDs to UUID format
      afterVersion13()  // Migrate feed IDs to UUID format
    )
  }
  
  private fun afterVersion12(): AfterVersion {
    return AfterVersion(12) { driver ->
      val ids = PostsIdsQuery(driver).executeAsList()
      ids.forEach { id -> migratePostLinkIdsToUuid(driver, id) }
    }
  }
  
  private fun migratePostLinkIdsToUuid(driver: SqlDriver, oldPostId: String) {
    val newPostId = nameBasedUuidOf(oldPostId).toString()
    driver.execute(
      identifier = null,
      sql = "UPDATE post SET id = ? WHERE id = ?",
      parameters = 2,
      binders = {
        bindString(0, newPostId)
        bindString(1, oldPostId)
      }
    )
    // Also update related tables (bookmark, post_search, etc.)
  }
}
```

### SQLDelight Schema

#### Core Tables

```sql
-- Feed table (từ Feed.sq)
CREATE TABLE feed(
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  icon TEXT NOT NULL,
  description TEXT NOT NULL,
  link TEXT NOT NULL,
  homepageLink TEXT NOT NULL,
  createdAt INTEGER AS Instant NOT NULL,
  pinnedAt INTEGER AS Instant,
  lastCleanUpAt INTEGER AS Instant,
  alwaysFetchSourceArticle INTEGER AS Boolean NOT NULL DEFAULT 0,
  pinnedPosition REAL NOT NULL DEFAULT 0.0,
  showFeedFavIcon INTEGER AS Boolean NOT NULL DEFAULT 1,
  lastUpdatedAt INTEGER AS Instant,
  refreshInterval TEXT AS Duration NOT NULL DEFAULT '1h'
);

-- Post table (từ Post.sq)
CREATE TABLE post(
  id TEXT NOT NULL,
  sourceId TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  rawContent TEXT,
  imageUrl TEXT,
  date INTEGER AS Instant NOT NULL,
  syncedAt INTEGER AS Instant NOT NULL,
  link TEXT NOT NULL,
  commentsLink TEXT DEFAULT NULL,
  bookmarked INTEGER AS Boolean NOT NULL DEFAULT 0,
  read INTEGER AS Boolean NOT NULL DEFAULT 0,
  isHidden INTEGER AS Boolean NOT NULL DEFAULT 0,
  PRIMARY KEY (id, sourceId),
  FOREIGN KEY(sourceId) REFERENCES feed(id) ON DELETE CASCADE
);

-- Bookmark table (từ Bookmark.sq)
CREATE TABLE bookmark(
  id TEXT NOT NULL PRIMARY KEY,
  title TEXT NOT NULL,
  link TEXT NOT NULL,
  date INTEGER AS Instant NOT NULL,
  FOREIGN KEY(id) REFERENCES post(id) ON DELETE CASCADE
);

-- Feed Group table (từ FeedGroup.sq)
CREATE TABLE feedGroup(
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  createdAt INTEGER AS Instant NOT NULL,
  updatedAt INTEGER AS Instant NOT NULL,
  pinnedAt INTEGER AS Instant
);

-- Feed Group Feed junction table (từ FeedGroupFeed.sq)
CREATE TABLE feedGroupFeed(
  groupId TEXT NOT NULL,
  feedId TEXT NOT NULL,
  PRIMARY KEY(groupId, feedId),
  FOREIGN KEY(groupId) REFERENCES feedGroup(id) ON DELETE CASCADE,
  FOREIGN KEY(feedId) REFERENCES feed(id) ON DELETE CASCADE
);
```

#### Full-Text Search Support

```sql
-- FTS table for feed search
CREATE VIRTUAL TABLE feed_search_fts USING fts5(
  id UNINDEXED,
  name,
  description,
  content=feed,
  content_rowid=rowid
);

-- Triggers for FTS sync
CREATE TRIGGER feed_search_fts_insert AFTER INSERT ON feed 
BEGIN
  INSERT INTO feed_search_fts(rowid, id, name, description) 
  VALUES (new.rowid, new.id, new.name, new.description);
END;
```

### Database Adapters & Type Safety

```kotlin
// Date adapter for kotlinx.datetime integration
object DateAdapter : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)
  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

// Database configuration with adapters
@Provides
@AppScope
fun providesDatabase(driver: SqlDriver): ReaderDatabase {
  return ReaderDatabase(
    driver = driver,
    postAdapter = Post.Adapter(dateAdapter = DateAdapter),
    feedAdapter = Feed.Adapter(createdAtAdapter = DateAdapter),
    bookmarkAdapter = Bookmark.Adapter(dateAdapter = DateAdapter)
  )
}
```

## Network Architecture

### HTTP Client Configuration

```kotlin
fun <T : HttpClientEngineConfig> httpClient(
  engine: HttpClientEngineFactory<T>,
  config: T.() -> Unit
): HttpClient {
  return HttpClient(engine) {
    followRedirects = false
    
    install(HttpCache) // Response caching
    
    install(Logging) {
      level = LogLevel.INFO
      logger = KermitLogger.asHttpClientLogger()
    }
    
    install(ContentNegotiation) {
      json(Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
      })
    }
    
    engine(config)
  }
}
```

### Platform-Specific Network Implementation

#### Android (OkHttp)
```kotlin
@Provides
@AppScope
fun providesHttpClient(): HttpClient = httpClient(OkHttp) {
  config {
    protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
    connectTimeout(30, TimeUnit.SECONDS)
    readTimeout(60, TimeUnit.SECONDS)
  }
}
```

#### iOS (Darwin/NSURLSession)
```kotlin
@Provides  
@AppScope
fun providesHttpClient(): HttpClient = httpClient(Darwin) {
  configureRequest {
    setAllowsCellularAccess(true)
    setAllowsConstrainedNetworkAccess(false)
  }
}
```

### Feed Parsing Architecture

```kotlin
// Parser abstraction
interface FeedContentParser<T> {
  fun canParse(source: String): Boolean
  suspend fun parse(source: String): T
}

// Concrete implementations
class RSSContentParser : FeedContentParser<FeedPayload>
class AtomContentParser : FeedContentParser<FeedPayload>  
class JSONFeedContentParser : FeedContentParser<FeedPayload>
class RDFContentParser : FeedContentParser<FeedPayload>

// Parser coordinator
@Inject
class FeedFetcher(
  private val httpClient: HttpClient,
  private val parsers: Set<FeedContentParser<FeedPayload>>
) {
  suspend fun fetchFeed(url: String): FeedFetchResult {
    val response = httpClient.get(url)
    val content = response.bodyAsText()
    
    val parser = parsers.firstOrNull { it.canParse(content) }
      ?: return FeedFetchResult.Error("Unsupported feed format")
      
    return try {
      val payload = parser.parse(content)
      FeedFetchResult.Success(payload)
    } catch (e: Exception) {
      FeedFetchResult.Error(e.message ?: "Parse error")
    }
  }
}
```

## Dependency Injection Architecture

### Component Hierarchy

```kotlin
// App-level component (platform-specific)
@AppScope
@Component  
abstract class ApplicationComponent(
  @get:Provides val context: Context // Android
  // OR
  @get:Provides val uiViewControllerProvider: () -> UIViewController // iOS
) : SharedApplicationComponent()

// Shared component across platforms
abstract class SharedApplicationComponent : 
  DataComponent,
  NetworkComponent, 
  LoggingComponent {
  
  // Factory methods for presenters
  abstract val homePresenterFactory: HomePresenterFactory
  abstract val readerPresenterFactory: ReaderPresenterFactory
  // ... other presenter factories
}

// Layer-specific components
interface DataComponent : SqlDriverPlatformComponent, DataStorePlatformComponent {
  @Provides @AppScope
  fun providesDatabase(driver: SqlDriver): ReaderDatabase
  
  @Provides @AppScope  
  fun providesRssRepository(/* deps */): RssRepository
}

interface NetworkComponent {
  @Provides @AppScope
  fun providesHttpClient(): HttpClient
  
  @Provides @AppScope
  fun providesFeedFetcher(/* deps */): FeedFetcher
}
```

### Scope Management

```kotlin
// Application scope - singleton across app lifecycle
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

// Activity scope - tied to activity/screen lifecycle  
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ActivityScope
```

### Factory Pattern for Presenters

```kotlin
// Factory typedef for type safety
internal typealias HomePresenterFactory = (
  ComponentContext,
  onPostClick: (PostWithMetadata) -> Unit,
  onFeedClick: (String) -> Unit
) -> HomePresenter

// Factory implementation
@Inject
class HomePresenter(
  private val rssRepository: RssRepository,
  private val settingsRepository: SettingsRepository,
  @Assisted componentContext: ComponentContext,
  @Assisted private val onPostClick: (PostWithMetadata) -> Unit,
  @Assisted private val onFeedClick: (String) -> Unit
) : ComponentContext by componentContext
```

## UI Architecture 

### Presenter Pattern (MVP-like)

```kotlin
@Inject
class HomePresenter(
  dispatchersProvider: DispatchersProvider,
  private val rssRepository: RssRepository,
  @Assisted componentContext: ComponentContext,
  @Assisted private val onPostClick: (PostWithMetadata) -> Unit
) : ComponentContext by componentContext {

  // Presenter instance with lifecycle management
  private val presenterInstance = instanceKeeper.getOrCreate {
    PresenterInstance(dispatchersProvider, rssRepository)
  }

  // Public state exposure
  internal val state: StateFlow<HomeState> = presenterInstance.state
  internal val effects = presenterInstance.effects.asSharedFlow()

  // Event handling
  fun dispatch(event: HomeEvent) {
    when (event) {
      is HomeEvent.OnPostClicked -> onPostClick(event.post)
      else -> presenterInstance.dispatch(event)
    }
  }

  // Internal presenter logic
  private class PresenterInstance(
    private val dispatchersProvider: DispatchersProvider,
    private val rssRepository: RssRepository
  ) : InstanceKeeper.Instance {
    
    private val coroutineScope = CoroutineScope(
      dispatchersProvider.main + SupervisorJob()
    )
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    private val _effects = MutableSharedFlow<HomeEffect>()
    val effects = _effects.asSharedFlow()
    
    fun dispatch(event: HomeEvent) {
      when (event) {
        HomeEvent.Refresh -> refreshFeeds()
        is HomeEvent.OnPostBookmarkClicked -> toggleBookmark(event.post)
        // ... handle other events
      }
    }
    
    private fun refreshFeeds() {
      coroutineScope.launch {
        _state.update { it.copy(isRefreshing = true) }
        try {
          rssRepository.refreshFeeds()
        } catch (e: Exception) {
          _effects.emit(HomeEffect.ShowError(e.message))
        } finally {
          _state.update { it.copy(isRefreshing = false) }
        }
      }
    }
    
    override fun onDestroy() {
      coroutineScope.cancel()
    }
  }
}
```

### Navigation with Decompose

```kotlin
@Inject
class AppPresenter(
  componentContext: ComponentContext,
  // ... presenter factories
) : ComponentContext by componentContext {

  private val navigation = StackNavigation<Config>()
  private val modalNavigation = SlotNavigation<ModalConfig>()

  // Screen stack management
  internal val screenStack: Value<ChildStack<*, Screen>> =
    childStack(
      source = navigation,
      serializer = Config.serializer(),
      initialConfiguration = Config.Home,
      handleBackButton = true,
      childFactory = ::createScreen
    )

  // Modal stack management  
  internal val modalStack: Value<ChildSlot<*, Modals>> =
    childSlot(
      source = modalNavigation,
      serializer = ModalConfig.serializer(),
      handleBackButton = true,
      childFactory = ::createModal
    )

  private fun createScreen(config: Config, componentContext: ComponentContext): Screen =
    when (config) {
      Config.Home -> Screen.Home(
        presenter = homePresenterFactory(
          componentContext,
          onPostClick = { post -> openPost(post) },
          onFeedClick = { feedId -> openFeed(feedId) }
        )
      )
      is Config.Reader -> Screen.Reader(
        presenter = readerPresenterFactory(config.args, componentContext) {
          navigation.pop()
        }
      )
      // ... other screens
    }

  @Serializable
  sealed interface Config {
    @Serializable object Home : Config
    @Serializable data class Reader(val args: ReaderArgs) : Config
    @Serializable object Search : Config
    // ... other configs
  }
}
```

### Compose UI Patterns

#### State-driven UI
```kotlin
@Composable
internal fun HomeScreen(homePresenter: HomePresenter) {
  val state by homePresenter.state.collectAsState()
  val posts = state.posts.collectAsLazyPagingItems()
  
  LaunchedEffect(homePresenter) {
    homePresenter.effects.collect { effect ->
      when (effect) {
        is HomeEffect.ShowError -> {
          // Show snackbar or dialog
        }
      }
    }
  }
  
  LazyColumn {
    items(
      count = posts.itemCount,
      key = posts.itemKey { it.id },
      contentType = posts.itemContentType { "Post" }
    ) { index ->
      val post = posts[index]
      if (post != null) {
        PostItem(
          post = post,
          onPostClick = { homePresenter.dispatch(HomeEvent.OnPostClicked(post)) },
          onBookmarkClick = { homePresenter.dispatch(HomeEvent.OnPostBookmarkClicked(post)) }
        )
      }
    }
  }
}
```

#### Reactive data flow
```kotlin
@Composable
internal fun PostItem(
  post: PostWithMetadata,
  onPostClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.clickable { onPostClick() },
    colors = CardDefaults.cardColors(
      containerColor = if (post.read) {
        AppTheme.colorScheme.surfaceContainerLow
      } else {
        AppTheme.colorScheme.surface
      }
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = post.title,
        style = MaterialTheme.typography.titleMedium,
        color = AppTheme.colorScheme.onSurface
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      Text(
        text = post.description,
        style = MaterialTheme.typography.bodyMedium,
        color = AppTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        IconButton(onClick = onBookmarkClick) {
          Icon(
            imageVector = if (post.bookmarked) {
              TwineIcons.Bookmark
            } else {
              TwineIcons.BookmarkBorder
            },
            contentDescription = null
          )
        }
      }
    }
  }
}
```

## Platform Integration

### Android Integration

#### MainActivity Setup
```kotlin
class MainActivity : ComponentActivity() {
  
  @Inject
  lateinit var applicationComponent: ApplicationComponent
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    setContent {
      AppTheme {
        App(
          onShareLink = { link -> shareLink(link) },
          onOpenLink = { link -> openLink(link) }
        )
      }
    }
  }
  
  private fun shareLink(link: String) {
    val shareIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, link)
      type = "text/plain"
    }
    startActivity(Intent.createChooser(shareIntent, "Share"))
  }
}
```

#### Android-specific DI
```kotlin
@Component
@AppScope
abstract class ApplicationComponent(
  @get:Provides val context: Context
) : SharedApplicationComponent() {
  
  @Provides
  @AppScope
  fun providesAppInfo(context: Context): AppInfo {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return AppInfo(
      versionCode = packageInfo.versionCode,
      versionName = packageInfo.versionName,
      isDebugBuild = BuildConfig.DEBUG
    )
  }
}
```

### iOS Integration

#### SwiftUI Bridge
```swift
// ContentView.swift
struct ContentView: View {
    let homeViewControllerComponent: InjectHomeViewControllerComponent
    let backDispatcher: BackDispatcher
    
    var body: some View {
        ComposeView(
            homeViewControllerComponent: homeViewControllerComponent,
            backDispatcher: backDispatcher
        )
        .ignoresSafeArea(.keyboard)
        .edgesIgnoringSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let homeViewControllerComponent: InjectHomeViewControllerComponent
    let backDispatcher: BackDispatcher
    
    func makeUIViewController(context: Context) -> UIViewController {
        return homeViewControllerComponent.homeViewControllerFactory(backDispatcher)
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed
    }
}
```

#### Compose-iOS Integration
```kotlin
// HomeViewController.kt (iosMain)
@OptIn(ExperimentalDecomposeApi::class)
@Inject
fun HomeViewController(
  app: App,
  @Assisted backDispatcher: BackDispatcher
): UIViewController {
  return ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
  ) {
    PredictiveBackGestureOverlay(
      backDispatcher = backDispatcher,
      backIcon = null,
      modifier = Modifier.fillMaxSize()
    ) {
      app(
        onShareLink = { /* iOS share implementation */ },
        onOpenLink = { /* iOS URL opening */ }
      )
    }
  }
}
```

## Performance Optimizations

### Database Performance

#### Indexing Strategy
```sql
-- Performance indexes
CREATE INDEX idx_post_source_id ON post(source_id);
CREATE INDEX idx_post_date ON post(date DESC);
CREATE INDEX idx_post_read ON post(read);
CREATE INDEX idx_bookmark_date ON bookmark(date DESC);
CREATE INDEX idx_feed_pinned_at ON feed(pinned_at) WHERE pinned_at IS NOT NULL;
```

#### Query Optimization
```kotlin
// Efficient pagination with cursor-based approach
fun postsPagingSource(sourceId: String?): PagingSource<Int, PostWithMetadata> {
  return QueryPagingSource(
    countQuery = if (sourceId != null) {
      postQueries.countPostsBySource(sourceId)
    } else {
      postQueries.countAllPosts()
    },
    transacter = postQueries,
    context = dispatchersProvider.io,
    queryProvider = { limit, offset ->
      if (sourceId != null) {
        postQueries.postsBySource(sourceId, limit, offset, ::mapToPostWithMetadata)
      } else {
        postQueries.allPosts(limit, offset, ::mapToPostWithMetadata)
      }
    }
  )
}
```

### Memory Management

#### Coroutine Scope Management
```kotlin
// Presenter with proper lifecycle management
private class PresenterInstance : InstanceKeeper.Instance {
  private val coroutineScope = CoroutineScope(
    dispatchersProvider.main + SupervisorJob()
  )
  
  // Properly scoped flows
  val posts = rssRepository.posts()
    .cachedIn(coroutineScope) // Cache in presenter scope
    .stateIn(
      scope = coroutineScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )
  
  override fun onDestroy() {
    coroutineScope.cancel() // Clean up resources
  }
}
```

#### Image Loading Optimization
```kotlin
// Coil configuration for optimal image loading
@Provides
@AppScope
fun providesImageLoader(context: Context): ImageLoader {
  return ImageLoader.Builder(context)
    .memoryCache {
      MemoryCache.Builder()
        .maxSizePercent(context, 0.15) // 15% of available memory
        .build()
    }
    .diskCache {
      DiskCache.Builder()
        .directory(context.cacheDir.resolve("image_cache"))
        .maxSizeBytes(50 * 1024 * 1024) // 50MB
        .build()
    }
    .respectCacheHeaders(false)
    .build()
}
```

### Network Performance

#### Caching Strategy
```kotlin
// HTTP cache configuration
install(HttpCache) {
  publicStorage(FileStorage(cacheDir))
  privateStorage(FileStorage(privateCacheDir))
}

// Repository-level caching
class RssRepository {
  private val feedCache = mutableMapOf<String, FeedPayload>()
  private val cacheExpiry = 5.minutes
  
  suspend fun fetchFeed(url: String): FeedPayload {
    val cached = feedCache[url]
    if (cached != null && cached.isNotExpired()) {
      return cached
    }
    
    val fresh = feedFetcher.fetchFeed(url)
    feedCache[url] = fresh
    return fresh
  }
}
```

## Testing Strategy

### Unit Testing
```kotlin
class RssRepositoryTest {
  
  @Test
  fun `addFeed should insert feed and return success`() = runTest {
    // Given
    val feedUrl = "https://example.com/feed.xml"
    val mockFeedPayload = FeedPayload(/* test data */)
    
    // When
    val result = repository.addFeed(feedUrl)
    
    // Then
    assertThat(result).isInstanceOf<Result.Success<Feed>>()
    verify(feedQueries).insertFeed(any())
  }
}
```

### Integration Testing
```kotlin
@Test
fun `feed sync should update database with new posts`() = runTest {
  // Given
  val testFeed = createTestFeed()
  val mockResponse = createMockFeedResponse()
  
  mockWebServer.enqueue(MockResponse().setBody(mockResponse))
  
  // When
  repository.refreshFeeds()
  
  // Then
  val posts = repository.posts().first()
  assertThat(posts).hasSize(expectedPostCount)
}
```

### UI Testing  
```kotlin
@Test
fun `home screen should display posts correctly`() {
  composeTestRule.setContent {
    HomeScreen(mockHomePresenter)
  }
  
  composeTestRule
    .onNodeWithText("Test Post Title")
    .assertIsDisplayed()
    
  composeTestRule
    .onNodeWithContentDescription("Bookmark")
    .performClick()
    
  verify(mockHomePresenter).dispatch(
    HomeEvent.OnPostBookmarkClicked(any())
  )
}
```

## Deployment & CI/CD

### Build Configuration

#### Gradle Build Types
```kotlin
android {
  buildTypes {
    debug {
      isDebuggable = true
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
    
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
    
    create("benchmark") {
      initWith(getByName("release"))
      matchingFallbacks += listOf("release")
      isDebuggable = false
      signingConfig = signingConfigs.getByName("debug")
    }
  }
}
```

#### iOS Build Configuration
```swift
// Configuration/Release.xcconfig
SWIFT_OPTIMIZATION_LEVEL = -O
SWIFT_COMPILATION_MODE = wholemodule
GCC_OPTIMIZATION_LEVEL = s
ENABLE_BITCODE = NO
```

### CI Pipeline (GitHub Actions)
```yaml
name: CI
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '20'
      - name: Run tests
        run: ./gradlew test
      - name: Check formatting
        run: ./gradlew spotlessCheck
        
  build-android:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build APK
        run: ./gradlew assembleRelease
        
  build-ios:
    needs: test
    runs-on: macos-latest
    steps:
      - name: Build iOS
        run: xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp build
```

---

This technical documentation provides a comprehensive overview of the Twine RSS Reader architecture, covering all major aspects from high-level design to implementation details, performance considerations, and deployment strategies.
