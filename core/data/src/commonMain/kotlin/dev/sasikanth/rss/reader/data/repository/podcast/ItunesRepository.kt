/*
 * Copyright 2025 CoinDex
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

package dev.sasikanth.rss.reader.data.repository.podcast

import dev.sasikanth.rss.reader.core.network.podcast.ItunesApi
import dev.sasikanth.rss.reader.di.scopes.AppScope
import dev.sasikanth.rss.reader.util.DispatchersProvider
import dev.sasikanth.rss.reader.util.loadResource
import kotlinx.coroutines.flow.flowOn
import me.tatarka.inject.annotations.Inject

@Inject
@AppScope
class ItunesRepository(
    private val dispatchersProvider: DispatchersProvider,
    private val itunesApi: ItunesApi
) {
    fun getTopPodcast(country: String, limit: Int) =
        loadResource {
            itunesApi.getTopPodcast(country, limit)
        }.flowOn(dispatchersProvider.io)
}