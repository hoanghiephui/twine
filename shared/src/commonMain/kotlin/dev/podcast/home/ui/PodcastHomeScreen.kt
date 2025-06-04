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

package dev.podcast.home.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import dev.podcast.home.HomeEvent
import dev.podcast.home.PodcastHomePresenter
import dev.podcast.player.PlayerBottomSheet
import dev.sasikanth.rss.reader.home.ui.HomeScreenContentScaffold
import dev.sasikanth.rss.reader.home.ui.featuredPosts
import dev.sasikanth.rss.reader.platform.LocalLinkHandler
import dev.sasikanth.rss.reader.ui.AppTheme
import dev.sasikanth.rss.reader.utils.inverse
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

internal val BOTTOM_SHEET_PEEK_HEIGHT = 96.dp
private val BOTTOM_SHEET_CORNER_SIZE = 32.dp

@Composable
fun PodcastHomeScreen(
  modifier: Modifier,
  homePresenter: PodcastHomePresenter,
  useDarkTheme: Boolean = false,
  onBottomSheetStateChanged: (SheetValue) -> Unit,
  onBottomSheetHidden: (isHidden: Boolean) -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val state by homePresenter.state.collectAsState()
  // val feedsState by homePresenter.feedsPresenter.state.collectAsState()
  val linkHandler = LocalLinkHandler.current

  val posts = state.posts?.collectAsLazyPagingItems()
  val featuredPosts by featuredPosts(posts, state.homeViewMode).collectAsState(initial = persistentListOf())

  val listState = rememberLazyListState()
  val featuredPostsPagerState = rememberPagerState(pageCount = { featuredPosts.size })
  val bottomSheetState =
    rememberStandardBottomSheetState(
      initialValue = state.feedsSheetState,
      confirmValueChange = {
        if (it != SheetValue.Hidden) {
          homePresenter.dispatch(HomeEvent.PlayerSheetStateChanged(it))
        } else {
          homePresenter.dispatch(HomeEvent.PlayerSheetStateChanged(SheetValue.PartiallyExpanded))
        }

        onBottomSheetStateChanged(it)

        true
      }
    )
  val bottomSheetScaffoldState =
    rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

  val bottomSheetProgress by bottomSheetState.progressAsState()
  val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  AppTheme(useDarkTheme = true) {
    Scaffold(modifier) { scaffoldPadding ->
      val density = LocalDensity.current
      val bottomPadding = scaffoldPadding.calculateBottomPadding()
      val targetSheetPeekHeight =
        remember(bottomPadding) { BOTTOM_SHEET_PEEK_HEIGHT + bottomPadding }
      var sheetPeekHeight by
        remember(targetSheetPeekHeight) { mutableStateOf(targetSheetPeekHeight) }

      // Since `animateScrollToItem` doesn't trigger nested scroll connection
      // we are manually animating the sheet peek height back to target sheet peek height
      var scrollToTopClicked by remember { mutableStateOf(false) }
      val scrollToTopAnimatedSheetPeekHeight by
        animateDpAsState(
          targetValue = if (scrollToTopClicked) targetSheetPeekHeight else 0.dp,
          finishedListener = { scrollToTopClicked = false }
        )

      LaunchedEffect(scrollToTopAnimatedSheetPeekHeight) {
        if (scrollToTopClicked) {
          sheetPeekHeight = scrollToTopAnimatedSheetPeekHeight
        }
      }

      LaunchedEffect(sheetPeekHeight) { onBottomSheetHidden(sheetPeekHeight == 0.dp) }

      val nestedScrollConnection = remember {
        object : NestedScrollConnection {
          override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
          ): Offset {
            val delta = consumed.y.toInt()
            val sheetPeekHeightInPx = with(density) { sheetPeekHeight.roundToPx() }
            val newSheetPeekHeight = sheetPeekHeightInPx + delta * 2

            sheetPeekHeight =
              with(density) {
                newSheetPeekHeight.coerceIn(0, targetSheetPeekHeight.roundToPx()).toDp()
              }

            return Offset.Zero
          }
        }
      }

      BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        content = { bottomSheetScaffoldContentPadding ->
          AppTheme(useDarkTheme = useDarkTheme) {
            Box(modifier = Modifier.fillMaxSize().background(AppTheme.colorScheme.backdrop)) {
              val hasFeeds = state.hasFeeds
              val swipeRefreshState =
                rememberPullRefreshState(
                  refreshing = state.isRefreshing,
                  onRefresh = { homePresenter.dispatch(HomeEvent.OnSwipeToRefresh) }
                )
              val canSwipeToRefresh = hasFeeds == true

              HomeScreenContentScaffold(
                modifier =
                  Modifier.pullRefresh(state = swipeRefreshState, enabled = canSwipeToRefresh),
                homeTopAppBar = {
                  HomeTopAppBar(
                    source = state.activeSource,
                    currentDateTime = state.currentDateTime,
                    postsType = state.postsType,
                    listState = listState,
                    hasFeeds = hasFeeds,
                    hasUnreadPosts = state.hasUnreadPosts,
                    onSearchClicked = { homePresenter.dispatch(HomeEvent.SearchClicked) },
                    onBookmarksClicked = { homePresenter.dispatch(HomeEvent.BookmarksClicked) },
                    onSettingsClicked = { homePresenter.dispatch(HomeEvent.SettingsClicked) },
                    onPostTypeChanged = {},
                    onMarkPostsAsRead = {},
                    onRssFeedClicked = { homePresenter.dispatch(HomeEvent.SwitchToRssFeed) }
                  )
                },
                body = { paddingValues -> Box(modifier = Modifier.fillMaxSize()) {} }
              )
            }
          }
        },
        sheetContent = {
          PlayerBottomSheet(
            bottomSheetProgress = bottomSheetProgress,
            closeSheet = { coroutineScope.launch { bottomSheetState.partialExpand() } },
          )
        },
        containerColor = Color.Transparent,
        sheetContainerColor = AppTheme.colorScheme.tintedBackground,
        sheetContentColor = AppTheme.colorScheme.tintedForeground,
        sheetShadowElevation = 0.dp,
        sheetTonalElevation = 0.dp,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape =
          RoundedCornerShape(
            topStart = BOTTOM_SHEET_CORNER_SIZE * bottomSheetProgress.inverse(),
            topEnd = BOTTOM_SHEET_CORNER_SIZE * bottomSheetProgress.inverse()
          ),
        sheetSwipeEnabled = true, // TODO cái này thiết lập lại khi có bài hát
        sheetDragHandle = null
      )
    }
  }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private fun SheetState.progressAsState(): State<Float> {
  return derivedStateOf {
    when {
      currentValue == SheetValue.Expanded && targetValue == SheetValue.Expanded -> 1f
      currentValue == SheetValue.Expanded && targetValue == SheetValue.PartiallyExpanded ->
        1f - anchoredDraggableState.progress
      currentValue == SheetValue.PartiallyExpanded && targetValue == SheetValue.PartiallyExpanded ->
        if (anchoredDraggableState.progress == 1f) 0f else anchoredDraggableState.progress
      currentValue == SheetValue.PartiallyExpanded && targetValue == SheetValue.Expanded ->
        anchoredDraggableState.progress
      else -> 0f
    }
  }
}
