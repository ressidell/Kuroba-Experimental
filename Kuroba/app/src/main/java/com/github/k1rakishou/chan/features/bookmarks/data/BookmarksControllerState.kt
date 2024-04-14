package com.github.k1rakishou.chan.features.bookmarks.data

sealed class BookmarksControllerState {
  data object Loading : BookmarksControllerState()
  data object Empty : BookmarksControllerState()
  data class NothingFound(val searchQuery: String) : BookmarksControllerState()
  data class Error(val errorText: String) : BookmarksControllerState()

  data class Data(
    val isReorderingMode: Boolean,
    val viewThreadBookmarksGridMode: Boolean,
    val groupedBookmarks: List<GroupOfThreadBookmarkItemViews>
  ) : BookmarksControllerState()
}