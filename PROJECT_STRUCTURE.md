# Twine - Project Structure

Twine is a cross-platform RSS reader application built with Kotlin Multiplatform and Compose Multiplatform. This document provides a comprehensive overview of the project structure and architecture.

## Overview

```
twine/
├── androidApp/              # Android-specific application code
├── iosApp/                  # iOS-specific application code  
├── shared/                  # Shared business logic across platforms
├── core/                    # Core modules and libraries
├── resources/               # Shared resources (icons, assets)
├── gradle/                  # Gradle wrapper and configuration
├── readme_images/           # Documentation images
├── release/                 # Release-related files
├── spotless/                # Code formatting configuration
└── build files             # Build configuration files
```

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Compose Multiplatform
- **Platforms**: Android, iOS
- **Database**: SQLDelight
- **Networking**: Ktor
- **Dependency Injection**: Kotlin Inject
- **Build System**: Gradle with Kotlin DSL
- **Code Formatting**: Spotless with ktfmt

## Platform-Specific Modules

### Android App (`androidApp/`)
Contains Android-specific application code and entry points.

```
androidApp/
├── src/
│   ├── androidDebug/        # Debug build variant resources
│   │   └── res/
│   └── androidMain/         # Main Android application code
│       ├── kotlin/          # Kotlin source files
│       └── res/             # Android resources
└── build.gradle.kts         # Android app build configuration
```

### iOS App (`iosApp/`)
Contains iOS-specific application code and Xcode project.

```
iosApp/
├── Configuration/           # iOS app configuration
├── TwineWidget/            # iOS widget implementation
│   └── Assets.xcassets/    # Widget assets
├── iosApp/                 # Main iOS app
│   ├── Assets.xcassets/    # App assets and icons
│   └── Preview Content/    # SwiftUI preview assets
└── iosApp.xcodeproj/       # Xcode project file
```

## Shared Module (`shared/`)
Contains the core business logic shared across all platforms.

```
shared/
├── src/
│   ├── androidMain/         # Android-specific shared code
│   │   └── kotlin/
│   ├── commonMain/          # Platform-agnostic shared code
│   │   ├── composeResources/ # Compose resources
│   │   └── kotlin/
│   │       └── dev/sasikanth/rss/reader/
│   │           ├── about/           # About screen
│   │           ├── addfeed/         # Add feed functionality
│   │           ├── app/             # Main app component
│   │           ├── billing/         # In-app billing
│   │           ├── blockedwords/    # Content filtering
│   │           ├── bookmarks/       # Bookmarking system
│   │           ├── components/      # Reusable UI components
│   │           ├── di/              # Dependency injection
│   │           ├── favicons/        # Favicon handling
│   │           ├── feed/            # Individual feed management
│   │           ├── feeds/           # Feed listing and management
│   │           ├── group/           # Feed grouping
│   │           ├── groupselection/  # Group selection UI
│   │           ├── home/            # Home screen
│   │           ├── initializers/    # App initializers
│   │           ├── logging/         # Logging infrastructure
│   │           ├── markdown/        # Markdown rendering
│   │           ├── opml/            # OPML import/export
│   │           ├── placeholder/     # Placeholder components
│   │           ├── platform/        # Platform abstractions
│   │           ├── posts/           # Post/article management
│   │           ├── premium/         # Premium features
│   │           ├── reader/          # Article reader
│   │           ├── search/          # Search functionality
│   │           ├── settings/        # App settings
│   │           ├── share/           # Sharing functionality
│   │           ├── ui/              # UI utilities and themes
│   │           └── utils/           # Utility functions
│   ├── commonTest/          # Shared unit tests
│   │   └── kotlin/
│   └── iosMain/             # iOS-specific shared code
│       └── kotlin/
└── build.gradle.kts         # Shared module build configuration
```

## Core Modules (`core/`)
Modular architecture with separate concerns.

### Base Module (`core/base/`)
Foundation utilities and common functionality.

```
core/base/
├── src/
│   ├── androidMain/         # Android-specific base code
│   ├── commonMain/          # Common base utilities
│   │   └── kotlin/
│   │       └── dev/sasikanth/rss/reader/
│   │           ├── api/             # API interfaces
│   │           ├── exceptions/      # Custom exceptions
│   │           ├── platform/        # Platform abstractions
│   │           └── util/            # Utility functions
│   ├── commonTest/          # Base module tests
│   └── iosMain/             # iOS-specific base code
└── build.gradle.kts
```

### Model Module (`core/model/`)
Data models and DTOs.

```
core/model/
├── src/
│   └── commonMain/
│       └── kotlin/
│           └── dev/sasikanth/rss/reader/core/model/
│               ├── local/           # Local data models
│               │   ├── Post.kt
│               │   ├── Feed.kt
│               │   ├── FeedGroup.kt
│               │   ├── Source.kt
│               │   └── ...
│               └── remote/          # Remote API models
│                   └── freshrss/    # FreshRSS API models
└── build.gradle.kts
```

### Network Module (`core/network/`)
Networking layer and API clients.

```
core/network/
├── http_requests/           # HTTP request examples/documentation
├── src/
│   ├── androidMain/         # Android-specific networking
│   ├── commonMain/          # Common networking code
│   │   └── kotlin/
│   │       └── dev/sasikanth/rss/reader/core/network/
│   │           ├── feed/            # Feed fetching
│   │           ├── parser/          # XML/RSS parsing
│   │           └── ...
│   ├── commonTest/          # Network module tests
│   └── iosMain/             # iOS-specific networking
└── build.gradle.kts
```

### Data Module (`core/data/`)
Data layer with repositories and database access.

```
core/data/
├── src/
│   ├── androidMain/         # Android-specific data layer
│   ├── commonMain/          # Common data layer
│   │   └── kotlin/
│   │       └── dev/sasikanth/rss/reader/core/data/
│   │           ├── repository/      # Repository implementations
│   │           ├── database/        # Database configuration
│   │           └── ...
│   └── iosMain/             # iOS-specific data layer
└── build.gradle.kts
```

## Resources (`resources/`)
Shared resources across platforms.

```
resources/
└── icons/                   # Custom icon resources
    ├── src/
    │   ├── androidMain/     # Android-specific icons
    │   ├── appleMain/       # iOS-specific icons
    │   ├── commonMain/      # Common icon definitions
    │   │   └── kotlin/
    │   │       └── dev/sasikanth/rss/reader/resources/icons/
    │   └── jvmMain/         # JVM-specific icons
    └── build.gradle.kts
```

## Build Configuration

### Root Level Files
- `build.gradle.kts` - Root build configuration with plugins and common settings
- `settings.gradle.kts` - Project structure and module definitions
- `gradle.properties` - Gradle properties and JVM settings
- `gradlew` / `gradlew.bat` - Gradle wrapper scripts

### Dependency Management
- `gradle/libs.versions.toml` - Version catalog with all dependencies
  - Versions for Kotlin, Compose, Android, and all libraries
  - Plugin configurations
  - Dependency bundles for common groups

### Code Quality
- `spotless/` - Code formatting configuration
- `.gitignore` - Git ignore patterns
- Code formatting enforced via Spotless with ktfmt Google style

## Key Features Implementation

### Cross-Platform Architecture
- **Shared Business Logic**: Core functionality in `shared/` module
- **Platform-Specific UI**: Android and iOS specific implementations
- **Platform Abstractions**: Common interfaces with platform-specific implementations

### Modular Design
- **Separation of Concerns**: Clear separation between UI, business logic, data, and network layers
- **Dependency Injection**: Kotlin Inject for clean dependency management
- **Feature Modules**: Each major feature has its own package structure

### RSS Reader Features
- **Feed Management**: Add, edit, remove, and organize RSS feeds
- **Feed Grouping**: Organize feeds into groups
- **Article Reading**: Built-in reader with full-text support
- **Bookmarking**: Save articles for later reading
- **Search**: Full-text search across articles
- **OPML Support**: Import/export feed lists
- **Background Sync**: Automatic feed updates
- **Material 3**: Modern UI with dynamic theming

### Database
- **SQLDelight**: Type-safe SQL with Kotlin multiplatform support
- **Local Storage**: Offline reading capability
- **Data Synchronization**: Background sync with conflict resolution

#### Database Schema (SQLDelight)
```
core/data/src/commonMain/sqldelight/dev/sasikanth/rss/reader/data/database/
├── Feed.sq                    # RSS feed definitions
├── Post.sq                    # Article/post storage
├── FeedGroup.sq              # Feed grouping
├── FeedGroupFeed.sq          # Feed-to-group relationships
├── FeedSearchFTS.sq          # Full-text search for feeds
├── User.sq                   # User preferences and settings
├── Bookmark.sq               # Bookmarked articles
├── BlockedWord.sq            # Content filtering
└── ... (additional schema files)
```

#### Database Features
- **Full-Text Search**: SQLite FTS for fast article searching
- **Relational Design**: Normalized schema for efficient storage
- **Cross-Platform**: Single schema shared between Android and iOS
- **Type Safety**: Generated Kotlin code from SQL definitions

### Networking
- **Ktor Client**: HTTP client for feed fetching
- **RSS/Atom Parsing**: Support for multiple feed formats
- **Smart Feed Discovery**: Automatic feed detection from websites

## CI/CD and GitHub Configuration (`.github/`)

```
.github/
├── FUNDING.yml                      # GitHub Sponsors configuration
├── ISSUE_TEMPLATE/                  # Issue templates
│   ├── bug_report.md               # Bug report template
│   └── feature_request.md          # Feature request template
└── workflows/                       # GitHub Actions workflows
    ├── ci_checks.yml               # Continuous integration checks
    ├── android_prod_release.yml    # Android production release
    └── ios_prod_release.yml        # iOS production release
```

### GitHub Actions Workflows
- **CI Checks**: Automated testing, linting, and code quality checks
- **Android Release**: Automated Android app building and publishing
- **iOS Release**: Automated iOS app building and publishing

## Configuration Files

### Root Level Configuration
- **`.gitignore`** - Git ignore patterns for build artifacts, IDE files
- **`LICENSE.txt`** - GNU GPL v3 license
- **`crowdin.yml`** - Crowdin localization configuration
- **`renovate.json5`** - Renovate dependency update configuration
- **`cleanup.sh`** - Project cleanup script

### Build and Dependencies
- **`gradle.properties`** - Gradle properties and JVM settings
- **`gradle/libs.versions.toml`** - Version catalog for dependency management
- **`gradle/wrapper/`** - Gradle wrapper files

### Code Quality and Formatting
- **`spotless/`** - Code formatting configuration files
- **`.idea/`** - IntelliJ IDEA configuration (partially gitignored)

## Localization and Resources

### Internationalization
- **Crowdin Integration**: Automated translation management
- **Compose Resources**: String resources using Compose Multiplatform resources
- **Multiple Languages**: Support for various languages via community translations

### Asset Management
- **`readme_images/`** - Documentation and marketing images
- **`resources/icons/`** - Vector icon definitions shared across platforms
- **Platform Resources**: Platform-specific resources in respective modules

## Development Setup

### Requirements
- **JDK 20+** - Required for Kotlin 2.2.0
- **Android Studio** - For Android development (Electric Eel or newer)
- **Xcode** - For iOS development (latest stable recommended)
- **Kotlin Multiplatform Mobile plugin** - IntelliJ/Android Studio plugin

### Environment Setup
```bash
# Clone the repository
git clone https://github.com/msasikanth/twine.git
cd twine

# Verify Java version
java -version  # Should be JDK 20+

# Make gradlew executable (if needed)
chmod +x gradlew
```

### Build Commands
```bash
# Format code (required before committing)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Build all modules
./gradlew build

# Run Android app
./gradlew :androidApp:installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean

# Build specific module
./gradlew :core:network:build
```

### IDE Configuration
- **Code Style**: Project uses ktfmt Google style enforced via Spotless
- **Copyright Headers**: Automatic insertion via Spotless configuration
- **Import Organization**: Configured in `.idea/` settings

## Contributing Guidelines

### Code Quality
- **Formatting**: Run `./gradlew spotlessApply` before committing
- **Testing**: Add tests for new functionality
- **Documentation**: Update relevant documentation for changes
- **Commits**: Use clear, descriptive commit messages

### Pull Request Process
1. Fork the repository
2. Create a feature branch
3. Make minimal, focused changes
4. Run tests and formatting
5. Submit pull request with clear description

This architecture provides a scalable, maintainable, and cross-platform solution for RSS reading while maintaining native performance and user experience on each platform through careful separation of concerns and modern development practices.