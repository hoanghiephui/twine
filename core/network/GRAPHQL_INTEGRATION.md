# Apollo GraphQL Integration

This document describes the Apollo GraphQL integration added to the Twine RSS Reader network layer.

## Overview

Apollo GraphQL has been integrated into the network module to provide GraphQL capabilities alongside the existing Ktor HTTP client for RSS feed fetching. This integration is designed to be non-intrusive and allows for future expansion to GraphQL-based RSS aggregation services or APIs.

## Components Added

### 1. Dependencies

- `apollo-runtime`: Core Apollo client runtime
- `apollo-api`: Apollo API types and utilities  
- Apollo Gradle plugin for code generation

### 2. GraphQL Schema (`core/network/src/commonMain/graphql/schema.graphqls`)

Defines the GraphQL schema for potential feed operations:
- `Query.feed`: Fetch feed information by URL
- `Query.searchFeeds`: Search for feeds  
- `Query.myFeeds`: Get user's subscribed feeds
- `Mutation.subscribeFeed`: Subscribe to a feed
- `Mutation.unsubscribeFeed`: Unsubscribe from a feed

### 3. GraphQL Operations (`core/network/src/commonMain/graphql/FeedOperations.graphql`)

Sample GraphQL queries and mutations for:
- Getting feed details
- Searching feeds
- Managing feed subscriptions

### 4. Apollo Client Factory (`ApolloClientFactory.kt`)

Creates configured Apollo clients with:
- User-Agent header matching Twine's Ktor client
- Configurable server URL for different environments

### 5. GraphQL Service (`GraphQLService.kt`)

Service interface and implementation providing:
- Type-safe GraphQL operations
- Error handling with Result types
- Placeholder implementations for future development

### 6. Network Component Integration

Updated `NetworkComponent` to:
- Extend `GraphQLComponent` interface  
- Provide Apollo client alongside Ktor client
- Maintain existing Ktor functionality

## Usage

### Creating Apollo Client

```kotlin
val apolloClientFactory = ApolloClientFactory(appInfo)
val apolloClient = apolloClientFactory.createApolloClient("https://api.example.com/graphql")
```

### Using GraphQL Service

```kotlin
val graphQLService = GraphQLServiceImpl(apolloClient)

// Get feed information
val feedResult = graphQLService.getFeedUrl("https://example.com/feed.xml")

// Search feeds  
val searchResult = graphQLService.searchFeeds("kotlin", limit = 10)

// Subscribe to feed
val subscribeResult = graphQLService.subscribeFeed("https://example.com/feed.xml")
```

### Dependency Injection

The Apollo client and GraphQL service are integrated into the existing dependency injection system via `GraphQLComponent`:

```kotlin
interface NetworkComponent : GraphQLComponent {
  // Provides both Ktor HttpClient and Apollo client
}
```

## Configuration

### Apollo Configuration

In `build.gradle.kts`:

```kotlin
apollo {
  service("service") {
    packageName.set("dev.sasikanth.rss.reader.core.network.graphql")
  }
}
```

### GraphQL Endpoint

The GraphQL endpoint can be configured per environment:

```kotlin
// Development
apolloClient = factory.createApolloClient("https://dev-api.example.com/graphql")

// Production  
apolloClient = factory.createApolloClient("https://api.example.com/graphql")
```

## Testing

Integration tests are provided in `GraphQLIntegrationTest.kt` to verify:
- Apollo client creation
- GraphQL service initialization
- Service method functionality

## Future Enhancements

This Apollo GraphQL integration provides a foundation for:

1. **GraphQL RSS Aggregators**: Connect to services that provide RSS feeds via GraphQL
2. **Feed Discovery**: Use GraphQL APIs for discovering new feeds  
3. **User Sync**: Synchronize user feed subscriptions across devices
4. **Analytics**: Query feed and reading analytics
5. **Content Recommendations**: Get personalized feed recommendations

## Compatibility

- **Kotlin Multiplatform**: Works on both Android and iOS
- **Existing Functionality**: Does not interfere with current Ktor-based RSS fetching
- **Minimal Impact**: No changes to existing RSS parsing or feed management logic

## Migration Notes

This is an additive change that:
- ✅ Preserves all existing Ktor HTTP functionality
- ✅ Maintains current RSS/Atom/JSON feed parsing
- ✅ Does not modify existing API contracts
- ✅ Allows gradual adoption of GraphQL features

The integration is designed to coexist with the current network layer and can be adopted incrementally as GraphQL services become available.