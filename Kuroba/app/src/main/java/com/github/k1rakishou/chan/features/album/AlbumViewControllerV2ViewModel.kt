package com.github.k1rakishou.chan.features.album

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.chan.core.base.BaseViewModel
import com.github.k1rakishou.chan.core.di.component.viewmodel.ViewModelComponent
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import com.github.k1rakishou.chan.core.manager.ChanThreadManager
import com.github.k1rakishou.chan.core.manager.CurrentOpenedDescriptorStateManager
import com.github.k1rakishou.chan.utils.BackgroundUtils
import com.github.k1rakishou.chan.utils.requireParams
import com.github.k1rakishou.common.mutableListWithCap
import com.github.k1rakishou.common.toHashSetBy
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.source.cache.ChanCatalogSnapshotCache
import com.github.k1rakishou.model.source.cache.thread.ChanThreadsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AlbumViewControllerV2ViewModel(
  private val savedStateHandle: SavedStateHandle,
  private val currentOpenedDescriptorStateManager: CurrentOpenedDescriptorStateManager,
  private val chanThreadManager: ChanThreadManager,
  private val chanCatalogSnapshotCache: ChanCatalogSnapshotCache,
  private val chanThreadsCache: ChanThreadsCache
) : BaseViewModel() {
  private val _currentListenMode: AlbumViewControllerV2.ListenMode
    get() = savedStateHandle.requireParams<AlbumViewControllerV2.Params>().listenMode
  private val _initialImageFullUrl: String?
    get() = savedStateHandle.requireParams<AlbumViewControllerV2.Params>().initialImageFullUrl

  private val _currentDescriptor = MutableStateFlow<ChanDescriptor?>(null)

  private val _albumItems = mutableStateListOf<AlbumItemData>()
  val albumItems: List<AlbumItemData>
    get() = _albumItems

  override fun injectDependencies(component: ViewModelComponent) {
    component.inject(this)
  }

  override suspend fun onViewModelReady() {
    viewModelScope.launch {
      Logger.debug(TAG) { "currentListenMode: ${_currentListenMode}" }

      val currentDescriptorFlow = when (_currentListenMode) {
        AlbumViewControllerV2.ListenMode.Catalog -> {
          currentOpenedDescriptorStateManager.currentCatalogDescriptorFlow
            .map { catalogDescriptor -> catalogDescriptor as ChanDescriptor? }
        }
        AlbumViewControllerV2.ListenMode.Thread -> {
          currentOpenedDescriptorStateManager.currentThreadDescriptorFlow
            .map { threadDescriptor -> threadDescriptor as ChanDescriptor? }
        }
      }

      currentDescriptorFlow
        .collectLatest { currentDescriptor ->
          Logger.debug(TAG) { "currentDescriptor changed to '${currentDescriptor}'" }
          _currentDescriptor.value = currentDescriptor
        }
    }

    viewModelScope.launch(Dispatchers.IO) {
      _currentDescriptor
        .flatMapLatest { currentDescriptor ->
          BackgroundUtils.ensureBackgroundThread()
          Logger.debug(TAG) { "Got new descriptor: '${currentDescriptor}'" }

          if (currentDescriptor == null) {
            return@flatMapLatest flowOf(null)
          }

          val loadedChanDescriptor = when (currentDescriptor) {
            is ChanDescriptor.CompositeCatalogDescriptor -> ChanThreadManager.LoadedChanDescriptor.Composite(currentDescriptor)
            is ChanDescriptor.CatalogDescriptor -> ChanThreadManager.LoadedChanDescriptor.Regular(currentDescriptor)
            is ChanDescriptor.ThreadDescriptor -> ChanThreadManager.LoadedChanDescriptor.Regular(currentDescriptor)
          }

          val albumItems = getAlbumItemsForChanDescriptor(loadedChanDescriptor)
          Logger.debug(TAG) { "Got ${albumItems?.size ?: 0} initial album items" }

          if (!albumItems.isNullOrEmpty()) {
            applyAlbumItemsToUi(albumItems)
          }

          return@flatMapLatest chanThreadManager.chanDescriptorLoadFinishedEventsFlow
            .filter { loadedChanDescriptor ->
              when (currentDescriptor) {
                is ChanDescriptor.CompositeCatalogDescriptor -> {
                  if (loadedChanDescriptor !is ChanThreadManager.LoadedChanDescriptor.Composite) {
                    return@filter false
                  }

                  if (loadedChanDescriptor.compositeCatalogDescriptor != currentDescriptor) {
                    return@filter false
                  }

                  return@filter true
                }
                is ChanDescriptor.CatalogDescriptor,
                is ChanDescriptor.ThreadDescriptor -> {
                  if (loadedChanDescriptor !is ChanThreadManager.LoadedChanDescriptor.Regular) {
                    return@filter false
                  }

                  return@filter loadedChanDescriptor.loadedDescriptor == currentDescriptor
                }
              }
            }
        }
        .filterNotNull()
        .mapNotNull { loadedChanDescriptor -> getAlbumItemsForChanDescriptor(loadedChanDescriptor) }
        .collectLatest { newAlbumItems -> applyAlbumItemsToUi(newAlbumItems) }
    }
  }

  private suspend fun applyAlbumItemsToUi(newAlbumItems: List<AlbumItemData>) {
    withContext(Dispatchers.Main) {
      Logger.debug(TAG) { "Got ${newAlbumItems.size} album items in total" }

      val duplicateChecker = _albumItems
        .toHashSetBy(capacity = _albumItems.size) { albumItemData -> albumItemData.thumbnailImage }

      val capacity = (newAlbumItems.size - _albumItems.size).coerceAtLeast(16)
      val newAlbumItemsToAppendList = mutableListWithCap<AlbumItemData>(capacity)

      newAlbumItems.forEach { newAlbumItemData ->
        if (duplicateChecker.contains(newAlbumItemData.thumbnailImage)) {
          return@forEach
        }

        newAlbumItemsToAppendList += newAlbumItemData
      }

      Logger.debug(TAG) { "Got ${newAlbumItemsToAppendList.size} new album items in total" }
      _albumItems.addAll(newAlbumItemsToAppendList)

      // TODO: show "X new images" toast
    }
  }

  private fun getAlbumItemsForChanDescriptor(
    loadedChanDescriptor: ChanThreadManager.LoadedChanDescriptor
  ): List<AlbumItemData>? {
    BackgroundUtils.ensureBackgroundThread()
    Logger.debug(TAG) { "loadedChanDescriptor is '${loadedChanDescriptor}'" }

    val actualChanDescriptor = when (loadedChanDescriptor) {
      is ChanThreadManager.LoadedChanDescriptor.Composite -> {
        loadedChanDescriptor.compositeCatalogDescriptor
      }

      is ChanThreadManager.LoadedChanDescriptor.Regular -> {
        loadedChanDescriptor.loadedDescriptor
      }
    }

    val posts = kotlin.run {
      when (actualChanDescriptor) {
        is ChanDescriptor.ICatalogDescriptor -> {
          val catalogSnapshot = chanCatalogSnapshotCache.get(actualChanDescriptor)
          if (catalogSnapshot == null) {
            return@run emptyList()
          }

          return@run catalogSnapshot.catalogThreadDescriptorList
            .mapNotNull { threadDescriptor ->
              chanThreadsCache.getOriginalPostFromCache(threadDescriptor.toOriginalPostDescriptor())
            }
        }
        is ChanDescriptor.ThreadDescriptor -> {
          return@run chanThreadsCache.getThreadPosts(actualChanDescriptor)
        }
      }
    }

    if (posts.isEmpty()) {
      Logger.debug(TAG) { "Got no posts for '${loadedChanDescriptor}'" }
      return null
    }

    Logger.debug(TAG) { "Got ${posts.size} posts for '${loadedChanDescriptor}'" }

    return posts.flatMap { chanPost ->
      return@flatMap chanPost.postImages.mapNotNull { chanPostImage ->
        val actualThumbnailUrl = chanPostImage.actualThumbnailUrl
        if (actualThumbnailUrl == null) {
          return@mapNotNull null
        }

        // TODO: FilterOutHiddenImagesUseCase

        return@mapNotNull AlbumItemData(
          postDescriptor = chanPost.postDescriptor,
          thumbnailImage = actualThumbnailUrl.toString(),
          fullImage = chanPostImage.imageUrl?.toString()
        )
      }
    }
  }

  data class AlbumItemData(
    val postDescriptor: PostDescriptor,
    val thumbnailImage: String,
    val fullImage: String?
  )

  class ViewModelFactory @Inject constructor(
    private val currentOpenedDescriptorStateManager: CurrentOpenedDescriptorStateManager,
    private val chanThreadManager: ChanThreadManager,
    private val chanCatalogSnapshotCache: ChanCatalogSnapshotCache,
    private val chanThreadsCache: ChanThreadsCache
  ) : ViewModelAssistedFactory<AlbumViewControllerV2ViewModel> {
    override fun create(handle: SavedStateHandle): AlbumViewControllerV2ViewModel {
      return AlbumViewControllerV2ViewModel(
        savedStateHandle = handle,
        currentOpenedDescriptorStateManager = currentOpenedDescriptorStateManager,
        chanThreadManager = chanThreadManager,
        chanCatalogSnapshotCache = chanCatalogSnapshotCache,
        chanThreadsCache = chanThreadsCache,
      )
    }
  }

  companion object {
    private const val TAG = "AlbumViewControllerV2ViewModel"
  }

}