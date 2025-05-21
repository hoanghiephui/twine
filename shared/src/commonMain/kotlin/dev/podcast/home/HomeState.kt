@file:OptIn(ExperimentalMaterialApi::class)

package dev.podcast.home

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Immutable
import app.cash.paging.PagingData
import dev.sasikanth.rss.reader.core.model.local.PostWithMetadata
import dev.sasikanth.rss.reader.core.model.local.PostsType
import dev.sasikanth.rss.reader.core.model.local.Source
import dev.sasikanth.rss.reader.home.HomeLoadingState.Loading
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class HomeState(
  val posts: Flow<PagingData<PostWithMetadata>>?,
  val loadingState: HomeLoadingState,
  val feedsSheetState: SheetValue,
  val activeSource: Source?,
  val hasFeeds: Boolean?,
  val postsType: PostsType,
  val hasUnreadPosts: Boolean,
  val currentDateTime: LocalDateTime,
) {

  companion object {

    fun default(currentDateTime: LocalDateTime) =
      HomeState(
        posts = null,
        loadingState = HomeLoadingState.Idle,
        feedsSheetState = SheetValue.PartiallyExpanded,
        activeSource = null,
        hasFeeds = null,
        postsType = PostsType.ALL,
        hasUnreadPosts = false,
        currentDateTime = currentDateTime,
      )
  }

  val isRefreshing: Boolean
    get() = loadingState == Loading
}

sealed interface HomeLoadingState {
  data object Idle : HomeLoadingState

  data object Loading : HomeLoadingState

  data class Error(val errorMessage: String) : HomeLoadingState
}
