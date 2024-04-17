package com.github.k1rakishou.chan.core.manager

import com.github.k1rakishou.common.AppConstants
import com.github.k1rakishou.common.parallelForEachOrdered
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
  private val _searchInstances = GenericCacheSource<ChanDescriptor, SearchInstance>()

  fun listenForSearchQueryUpdates(chanDescriptor: ChanDescriptor): StateFlow<String?> {
    return getOrCreateSearchInstance(chanDescriptor).searchQuery
  }

  fun currentSearchQuery(chanDescriptor: ChanDescriptor): String? {
    return getOrCreateSearchInstance(chanDescriptor).searchQuery.value
  }

  suspend fun updateSearchQuery(
    chanDescriptor: ChanDescriptor,
    postDescriptors: List<PostDescriptor>,
    searchQuery: String?
  ): List<PostDescriptor> {
    if (searchQuery == null || searchQuery.length < AppConstants.MIN_QUERY_LENGTH) {
      getOrCreateSearchInstance(chanDescriptor).updateSearchQuery(
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

    getOrCreateSearchInstance(chanDescriptor).updateSearchQuery(
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

  private fun getOrCreateSearchInstance(chanDescriptor: ChanDescriptor): SearchInstance {
    return _searchInstances.getOrPut(chanDescriptor, { SearchInstance(chanDescriptor) })
  }

  private class SearchInstance(
    val chanDescriptor: ChanDescriptor
  ) {
    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?>
      get() = _searchQuery.asStateFlow()

    private val _matchedPostDescriptors = MutableStateFlow<List<PostDescriptor>>(emptyList())
    val matchedPostDescriptors: StateFlow<List<PostDescriptor>>
      get() = _matchedPostDescriptors.asStateFlow()

    fun updateSearchQuery(searchQuery: String?, matchedPostDescriptors: List<PostDescriptor>) {
      _searchQuery.value = searchQuery
      _matchedPostDescriptors.value = emptyList()
    }
  }

}