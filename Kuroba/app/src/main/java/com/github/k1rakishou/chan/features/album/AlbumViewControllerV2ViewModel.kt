package com.github.k1rakishou.chan.features.album

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.base.BaseViewModel
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.di.component.viewmodel.ViewModelComponent
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import com.github.k1rakishou.chan.core.manager.ChanThreadManager
import com.github.k1rakishou.chan.core.manager.CurrentOpenedDescriptorStateManager
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequestData
import com.github.k1rakishou.chan.ui.compose.image.PostImageThumbnailKey
import com.github.k1rakishou.chan.utils.BackgroundUtils
import com.github.k1rakishou.chan.utils.KurobaMediaType
import com.github.k1rakishou.chan.utils.asKurobaMediaType
import com.github.k1rakishou.chan.utils.requireParams
import com.github.k1rakishou.common.mutableListWithCap
import com.github.k1rakishou.common.toHashSetBy
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPostImage
import com.github.k1rakishou.model.source.cache.ChanCatalogSnapshotCache
import com.github.k1rakishou.model.source.cache.thread.ChanThreadsCache
import com.github.k1rakishou.model.util.ChanPostUtils
import com.github.k1rakishou.persist_state.PersistableChanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import java.util.Locale
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
  val albumItems: SnapshotStateList<AlbumItemData>
    get() = _albumItems

  val albumSpanCount = ChanSettings.albumSpanCount.listenForChanges()
    .asFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val albumLayoutGridMode = PersistableChanState.albumLayoutGridMode.listenForChanges()
    .asFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

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

        val thumbnailImage = ImageLoaderRequestData.Url(httpUrl = actualThumbnailUrl, CacheFileType.PostMediaThumbnail)
        val fullImage = chanPostImage.imageUrl
          ?.let { imageUrl -> ImageLoaderRequestData.Url(imageUrl, CacheFileType.PostMediaFull) }

        return@mapNotNull AlbumItemData(
          isCatalogMode = actualChanDescriptor.isCatalogDescriptor(),
          postDescriptor = chanPost.postDescriptor,
          thumbnailImage = thumbnailImage,
          fullImage = fullImage,
          albumItemPostData = AlbumItemPostData(
            threadSubject = when (actualChanDescriptor) {
              is ChanDescriptor.ICatalogDescriptor ->  ChanPostUtils.getTitle(chanPost, actualChanDescriptor)
              is ChanDescriptor.ThreadDescriptor -> null
            },
            mediaInfo = formatImageDetails(chanPostImage),
            aspectRatio = calculateAspectRatio(chanPostImage)
          ),
          mediaType = chanPostImage.extension.asKurobaMediaType()
        )
      }
    }
  }

  private fun calculateAspectRatio(chanPostImage: ChanPostImage): Float? {
    val imageWidth = chanPostImage.imageWidth
    val imageHeight = chanPostImage.imageHeight
    if (imageWidth <= 0 || imageHeight <= 0) {
      return null
    }

    return (imageWidth.toFloat() / imageHeight.toFloat()).coerceIn(MIN_RATIO, MAX_RATIO)
  }

  private fun formatImageDetails(postImage: ChanPostImage): String {
    if (postImage.isInlined) {
      return postImage.extension?.uppercase(Locale.ENGLISH) ?: ""
    }

    return buildString {
      append(postImage.extension?.uppercase(Locale.ENGLISH) ?: "")
      append(" ")
      append(postImage.imageWidth)
      append("x")
      append(postImage.imageHeight)
      append(" ")
      append(ChanPostUtils.getReadableFileSize(postImage.size))
    }
  }

  @Immutable
  data class AlbumItemData(
    val isCatalogMode: Boolean,
    val postDescriptor: PostDescriptor,
    val thumbnailImage: ImageLoaderRequestData,
    val fullImage: ImageLoaderRequestData?,
    val albumItemPostData: AlbumItemPostData?,
    val mediaType: KurobaMediaType,
  ) {
    val composeKey: String
      get() = "${postDescriptor.serializeToString()}_${thumbnailImage}"
    val albumItemDataKey: AlbumItemDataKey = AlbumItemDataKey(postDescriptor, thumbnailImage)
  }

  @Immutable
  data class AlbumItemDataKey(
    val postDescriptor: PostDescriptor,
    val thumbnailImage: ImageLoaderRequestData,
  ) : PostImageThumbnailKey

  @Immutable
  data class AlbumItemPostData(
    val threadSubject: String?,
    val mediaInfo: String?,
    val aspectRatio: Float?
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

    private const val MAX_RATIO = 2f
    private const val MIN_RATIO = .4f
  }

}