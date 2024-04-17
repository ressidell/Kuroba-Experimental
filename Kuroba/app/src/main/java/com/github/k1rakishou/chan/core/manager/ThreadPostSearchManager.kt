package com.github.k1rakishou.chan.core.manager

import com.github.k1rakishou.common.AppConstants
import com.github.k1rakishou.common.parallelForEachOrdered
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPost
import com.github.k1rakishou.model.source.cache.GenericCacheSource
import com.github.k1rakishou.model.source.cache.thread.ChanThreadsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThreadPostSearchManager(
  private val chanThreadsCache: ChanThreadsCache,
  private val postFilterManager: PostFilterManager,
  private val postHideManager: PostHideManager
) {
  private val _activeSearches = GenericCacheSource<ChanDescriptor, ActiveSearch>()

  fun listenForSearchQueryUpdates(chanDescriptor: ChanDescriptor): StateFlow<String?> {
    return getOrCreateSearch(chanDescriptor).searchQuery
  }

  fun listenForScrollAnchorPostDescriptor(chanDescriptor: ChanDescriptor): StateFlow<PostDescriptor?> {
    return getOrCreateSearch(chanDescriptor).currentPostDescriptor
  }

  fun currentSearchQuery(chanDescriptor: ChanDescriptor): String? {
    return getOrCreateSearch(chanDescriptor).searchQuery.value
  }

  fun goToPrevious(chanDescriptor: ChanDescriptor) {
    getOrCreateSearch(chanDescriptor).goToPrevious()
  }

  fun goToNext(chanDescriptor: ChanDescriptor) {
    getOrCreateSearch(chanDescriptor).goToNext()
  }

  suspend fun updateSearchQuery(
    chanDescriptor: ChanDescriptor,
    postDescriptors: List<PostDescriptor>,
    searchQuery: String?
  ): List<PostDescriptor> {
    if (searchQuery == null || searchQuery.length < AppConstants.MIN_QUERY_LENGTH) {
      getOrCreateSearch(chanDescriptor).updateSearchQuery(
        searchQuery = searchQuery,
        matchedPostDescriptors = emptyList()
      )

      return postDescriptors
    }

    val matchedPostDescriptors = parallelForEachOrdered<PostDescriptor, PostDescriptor?>(
      dataList = postDescriptors,
      dispatcher = Dispatchers.IO
    ) { _, postDescriptor ->
      if (postFilterManager.contains(postDescriptor)) {
        if (postFilterManager.getPostFilter(postDescriptor)?.isPostHiddenOrRemoved != true) {
          // Post is hidden or removed. Skip it.
          return@parallelForEachOrdered null
        }

        // Post is neither hidden nor removed. Process it further.
      }

      if (postHideManager.contains(postDescriptor)) {
        return@parallelForEachOrdered null
      }

      val chanPost = chanThreadsCache.getPostFromCache(postDescriptor)
        ?: return@parallelForEachOrdered null

      if (!checkMatchesQuery(chanPost, searchQuery)) {
        return@parallelForEachOrdered null
      }

      return@parallelForEachOrdered postDescriptor
    }.filterNotNull()

    getOrCreateSearch(chanDescriptor).updateSearchQuery(
      searchQuery = searchQuery,
      matchedPostDescriptors = matchedPostDescriptors
    )

    return matchedPostDescriptors
  }

  private fun checkMatchesQuery(chanPost: ChanPost, searchQuery: String): Boolean {
    if (chanPost.postComment.comment().contains(other = searchQuery, ignoreCase = true)) {
      return true
    }

    if (chanPost.subject?.contains(other = searchQuery, ignoreCase = true) == true) {
      return true
    }

    // TODO: New catalog/thread search. Add more matchers.
    return false
  }

  private fun getOrCreateSearch(chanDescriptor: ChanDescriptor): ActiveSearch {
    return _activeSearches.getOrPut(chanDescriptor, { ActiveSearch(chanDescriptor) })
  }

  private class ActiveSearch(
    val chanDescriptor: ChanDescriptor
  ) {
    private val tag by lazy(LazyThreadSafetyMode.NONE) { "ActiveSearch(${chanDescriptor})" }

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?>
      get() = _searchQuery.asStateFlow()

    private val _matchedPostDescriptors = MutableStateFlow<List<PostDescriptor>>(emptyList())
    val matchedPostDescriptors: StateFlow<List<PostDescriptor>>
      get() = _matchedPostDescriptors.asStateFlow()

    private val _currentPostDescriptor = MutableStateFlow<PostDescriptor?>(null)
    val currentPostDescriptor: StateFlow<PostDescriptor?>
      get() = _currentPostDescriptor.asStateFlow()

    fun updateSearchQuery(searchQuery: String?, matchedPostDescriptors: List<PostDescriptor>) {
      Logger.debug(tag) {
        "updateSearchQuery() searchQuery: '${searchQuery}', " +
          "matchedPostDescriptorsCount: ${matchedPostDescriptors.size}"
      }

      if (searchQuery.isNullOrEmpty()) {
        _matchedPostDescriptors.value = emptyList()
        _currentPostDescriptor.value = null
      } else {
        _matchedPostDescriptors.value = matchedPostDescriptors
        _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
      }

      _searchQuery.value = searchQuery
    }

    fun goToPrevious() {
      goToPost { currentPostDescriptor, matchedPostDescriptors ->
        val currentIndex = matchedPostDescriptors.indexOfLast { postDescriptor -> postDescriptor == currentPostDescriptor }
        if (currentIndex !in matchedPostDescriptors.indices) {
          Logger.debug(tag) { "goToPrevious() ${currentIndex} is not in ${matchedPostDescriptors.indices} range" }
          _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
          return@goToPost
        }

        var previousIndex = currentIndex - 1
        if (previousIndex < 0) {
          previousIndex = matchedPostDescriptors.lastIndex
        }

        Logger.debug(tag) {
          "goToPrevious() previousIndex: ${previousIndex}, " +
            "currentIndex: ${currentIndex}, " +
            "lastIndex: ${matchedPostDescriptors.lastIndex}"
        }

        val previousPostDescriptor = matchedPostDescriptors.getOrNull(previousIndex)
        if (previousPostDescriptor == null) {
          Logger.debug(tag) {
            "goToPrevious() previousPostDescriptor is null, " +
              "previousIndex: ${previousIndex}, " +
              "matchedPostDescriptors.indices: ${matchedPostDescriptors.indices}"
          }

          _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
          return@goToPost
        }

        Logger.debug(tag) { "goToPrevious() previousPostDescriptor: ${previousPostDescriptor}" }
        _currentPostDescriptor.value = previousPostDescriptor
      }
    }

    fun goToNext() {
      goToPost { currentPostDescriptor, matchedPostDescriptors ->
        val currentIndex = matchedPostDescriptors.indexOfFirst { postDescriptor -> postDescriptor == currentPostDescriptor }
        if (currentIndex !in matchedPostDescriptors.indices) {
          Logger.debug(tag) { "goToNext() ${currentIndex} is not in ${matchedPostDescriptors.indices} range" }
          _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
          return@goToPost
        }

        var nextIndex = currentIndex + 1
        if (nextIndex > matchedPostDescriptors.lastIndex) {
          nextIndex = 0
        }

        Logger.debug(tag) {
          "goToNext() nextIndex: ${nextIndex}, " +
            "currentIndex: ${currentIndex}, " +
            "lastIndex: ${matchedPostDescriptors.lastIndex}"
        }

        val nextPostDescriptor = matchedPostDescriptors.getOrNull(nextIndex)
        if (nextPostDescriptor == null) {
          Logger.debug(tag) {
            "goToNext() nextPostDescriptor is null, " +
              "nextIndex: ${nextIndex}, " +
              "matchedPostDescriptors.indices: ${matchedPostDescriptors.indices}"
          }

          _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
          return@goToPost
        }

        Logger.debug(tag) { "goToNext() nextPostDescriptor: ${nextPostDescriptor}" }
        _currentPostDescriptor.value = nextPostDescriptor
      }
    }

    private fun goToPost(block: (PostDescriptor, List<PostDescriptor>) -> Unit) {
      val searchQuery = _searchQuery.value
      val matchedPostDescriptors = _matchedPostDescriptors.value

      if (searchQuery == null || searchQuery.length < AppConstants.MIN_QUERY_LENGTH || matchedPostDescriptors.isEmpty()) {
        _currentPostDescriptor.value = null
        return
      }

      val currentPostDescriptor = _currentPostDescriptor.value
      if (currentPostDescriptor == null) {
        _currentPostDescriptor.value = matchedPostDescriptors.firstOrNull()
        return
      }

      block(currentPostDescriptor, matchedPostDescriptors)
    }
  }

}