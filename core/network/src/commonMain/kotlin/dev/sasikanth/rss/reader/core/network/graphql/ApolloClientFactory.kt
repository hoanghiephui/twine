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
package dev.sasikanth.rss.reader.core.network.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import dev.sasikanth.rss.reader.app.AppInfo
import me.tatarka.inject.annotations.Inject

@Inject
class ApolloClientFactory(private val appInfo: AppInfo) {

  fun createApolloClient(serverUrl: String): ApolloClient {
    return ApolloClient.Builder()
      .serverUrl(serverUrl)
      .addHttpInterceptor(UserAgentInterceptor(appInfo))
      .build()
  }

  private class UserAgentInterceptor(private val appInfo: AppInfo) : HttpInterceptor {
    override suspend fun intercept(
      request: HttpRequest,
      chain: HttpInterceptorChain
    ): HttpResponse {
      return chain.proceed(
        request.newBuilder()
          .addHeader("User-Agent", "Twine/${appInfo.versionName} (https://github.com/msasikanth/twine)")
          .build()
      )
    }
  }
}