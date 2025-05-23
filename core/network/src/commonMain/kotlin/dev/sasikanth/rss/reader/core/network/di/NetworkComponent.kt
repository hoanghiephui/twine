/*
 * Copyright 2023 Sasikanth Miriyampalli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sasikanth.rss.reader.core.network.di

import co.touchlab.kermit.Logger as KermitLogger
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.CallConverterFactory
import dev.sasikanth.rss.reader.core.network.podcast.ItunesApi
import dev.sasikanth.rss.reader.core.network.podcast.createItunesApi
import dev.sasikanth.rss.reader.di.scopes.AppScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier

expect interface NetworkComponent

@Qualifier annotation class PodcastApiService

fun <T : HttpClientEngineConfig> httpClient(
  engine: HttpClientEngineFactory<T>,
  config: T.() -> Unit
): HttpClient {
  return HttpClient(engine) {
    followRedirects = false

    engine { config() }

    install(HttpCache)

    install(Logging) {
      level = LogLevel.ALL
      logger =
        object : Logger {
          override fun log(message: String) {
            KermitLogger.v("HttpClient") { message.lines().joinToString { "\n\t\t$it" } }
          }
        }
    }
  }
}

@get:Provides
@get:AppScope
val json = Json {
  ignoreUnknownKeys = true
  isLenient = true
  explicitNulls = false
  encodeDefaults = true
  coerceInputValues = true
}

@Provides
@AppScope
@PodcastApiService
fun provideHttpClient(
  json: Json,
): HttpClient = HttpClient {
  install(ContentNegotiation) { json(json) }
  install(Logging) {
    logger =
      object : Logger {
        override fun log(message: String) {
          KermitLogger.v("Podcast HttpClient") { message.lines().joinToString { "\n\t\t$it" } }
        }
      }
    level = LogLevel.ALL
  }
  install(HttpTimeout) {
    requestTimeoutMillis = 60000
    socketTimeoutMillis = 60000
    connectTimeoutMillis = 60000
  }
}

@Provides
@AppScope
fun provideItunesApi(@PodcastApiService client: HttpClient): ItunesApi =
  Ktorfit.Builder()
    .converterFactories(CallConverterFactory())
    .httpClient(client)
    .baseUrl("https://itunes.apple.com/")
    .build()
    .createItunesApi()
