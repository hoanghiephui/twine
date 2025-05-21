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

package dev.podcast.home

import androidx.compose.material3.SheetValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnCreate
import dev.sasikanth.rss.reader.home.HomeState
import dev.sasikanth.rss.reader.util.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class PodcastHomePresenter(
  dispatchersProvider: DispatchersProvider,
  @Assisted componentContext: ComponentContext,
  @Assisted private val openSearch: () -> Unit,
  @Assisted private val openBookmarks: () -> Unit,
  @Assisted private val openSettings: () -> Unit,
  @Assisted private val openRssFeed: () -> Unit,
) : ComponentContext by componentContext {

  private val backCallback = BackCallback {
    if (state.value.feedsSheetState == SheetValue.Expanded) {
      dispatch(HomeEvent.BackClicked)
      return@BackCallback
    }
  }

  private val presenterInstance =
    instanceKeeper.getOrCreate {
      PresenterInstance(
        dispatchersProvider = dispatchersProvider,
      )
    }

  internal val state = presenterInstance.state

  init {
    lifecycle.doOnCreate {
      backHandler.register(backCallback)
      backCallback.isEnabled = false
    }
  }

  fun dispatch(event: HomeEvent) {
    when (event) {
      is HomeEvent.PlayerSheetStateChanged -> {
        backCallback.isEnabled = event.feedsSheetState == SheetValue.Expanded
      }
      is HomeEvent.SearchClicked -> openSearch()
      is HomeEvent.BookmarksClicked -> openBookmarks()
      is HomeEvent.SettingsClicked -> openSettings()
      is HomeEvent.SwitchToRssFeed -> openRssFeed()
      else -> {
        // no-op
      }
    }
    presenterInstance.dispatch(event)
  }

  private class PresenterInstance(
    dispatchersProvider: DispatchersProvider,
  ) : InstanceKeeper.Instance {

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchersProvider.main)
    private val scrolledPostItems = mutableSetOf<String>()

    private val defaultState =
      HomeState.default(
        currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
      )
    private val _state = MutableStateFlow(defaultState)
    val state: StateFlow<HomeState> =
      _state.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = defaultState
      )

    init {
      dispatch(HomeEvent.Init)
    }

    fun dispatch(event: HomeEvent) {
      when (event) {
        HomeEvent.Init -> init()
        HomeEvent.OnSwipeToRefresh -> refreshContent()
        is HomeEvent.PlayerSheetStateChanged -> feedsSheetStateChanged(event.feedsSheetState)
        is HomeEvent.BackClicked -> backClicked()
        is HomeEvent.SearchClicked -> {
          // not
        }
        is HomeEvent.SettingsClicked -> {
          // not
        }
        is HomeEvent.BookmarksClicked -> {
          // TODO
        }
        is HomeEvent.SwitchToRssFeed -> {
          // not
        }
      }
    }

    private fun feedsSheetStateChanged(feedsSheetState: SheetValue) {
      _state.update {
        // Clear search query once feeds sheet is collapsed
        if (feedsSheetState == SheetValue.PartiallyExpanded) {
          // feedsPresenter.dispatch(FeedsEvent.ClearSearchQuery)
        }

        it.copy(feedsSheetState = feedsSheetState)
      }
    }

    private fun backClicked() {
      coroutineScope.launch {
        _state.update { it.copy(feedsSheetState = SheetValue.PartiallyExpanded) }
      }
    }

    private fun init() {}

    private fun refreshContent() {}
  }
}

typealias PodcastHomePresenterFactory =
  (
    ComponentContext,
    openSearch: () -> Unit,
    openBookmarks: () -> Unit,
    openSettings: () -> Unit,
    openRssFeed: () -> Unit,
  ) -> PodcastHomePresenter
