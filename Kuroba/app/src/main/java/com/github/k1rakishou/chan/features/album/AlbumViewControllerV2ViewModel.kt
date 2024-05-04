package com.github.k1rakishou.chan.features.album

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.base.BaseViewModel
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.core.di.component.viewmodel.ViewModelComponent
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import com.github.k1rakishou.chan.core.manager.ChanThreadManager
import com.github.k1rakishou.chan.core.manager.CompositeCatalogManager
import com.github.k1rakishou.chan.core.manager.CurrentOpenedDescriptorStateManager
import com.github.k1rakishou.chan.ui.compose.image.ImageLoaderRequestData
import com.github.k1rakishou.chan.ui.compose.image.PostImageThumbnailKey
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope
import com.github.k1rakishou.chan.ui.compose.snackbar.manager.SnackbarManagerFactory
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.chan.utils.BackgroundUtils
import com.github.k1rakishou.chan.utils.KurobaMediaType
import com.github.k1rakishou.chan.utils.asKurobaMediaType
import com.github.k1rakishou.chan.utils.getAndConsume
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val appResources: AppResources,
  private val currentOpenedDescriptorStateManager: CurrentOpenedDescriptorStateManager,
  private val chanThreadManager: ChanThreadManager,
  private val chanCatalogSnapshotCache: ChanCatalogSnapshotCache,
  private val chanThreadsCache: ChanThreadsCache,
  private val snackbarManagerFactory: SnackbarManagerFactory,
  private val compositeCatalogManager: CompositeCatalogManager
) : BaseViewModel() {
  private var _skippedInitialLoad = false

  private val _currentListenMode: AlbumViewControllerV2.ListenMode
    get() = savedStateHandle.requireParams<AlbumViewControllerV2.Params>().listenMode

  private val _currentDescriptor = MutableStateFlow<ChanDescriptor?>(null)
  val currentDescriptor: StateFlow<ChanDescriptor?>
    get() = _currentDescriptor.asStateFlow()

  private val _albumItems = mutableStateListOf<AlbumItemData>()
  val albumItems: SnapshotStateList<AlbumItemData>
    get() = _albumItems

  private val _scrollToPosition = MutableSharedFlow<Int>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val scrollToPosition: SharedFlow<Int>
    get() = _scrollToPosition.asSharedFlow()

  private val _lastScrollPosition = mutableIntStateOf(0)
  val lastScrollPosition: IntState
    get() = _lastScrollPosition

  private val _toolbarData = MutableStateFlow<ToolbarData>(ToolbarData())
  val toolbarData: StateFlow<ToolbarData>
    get() = _toolbarData

  val albumSpanCount = ChanSettings.albumSpanCount.listenForChanges()
    .asFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val albumLayoutGridMode = PersistableChanState.albumLayoutGridMode.listenForChanges()
    .asFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

  private val snackbarManager by lazy {
    val snackbarScope = when (_currentListenMode) {
      AlbumViewControllerV2.ListenMode.Catalog -> SnackbarScope.Album(SnackbarScope.MainLayoutAnchor.Catalog)
      AlbumViewControllerV2.ListenMode.Thread -> SnackbarScope.Album(SnackbarScope.MainLayoutAnchor.Thread)
    }

    snackbarManagerFactory.snackbarManager(snackbarScope)
  }

  override fun injectDependencies(component: ViewModelComponent) {
    component.inject(this)
  }

  override suspend fun onViewModelReady() {
    viewModelScope.launch {
      chanThreadManager.awaitUntilDependenciesInitialized()

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

          if (currentDescriptor == null) {
            return@flatMapLatest flowOf(null)
          }

          Logger.debug(TAG) { "Got new descriptor: '${currentDescriptor}'" }

          val loadedChanDescriptor = when (currentDescriptor) {
            is ChanDescriptor.CompositeCatalogDescriptor -> ChanThreadManager.LoadedChanDescriptor.Composite(currentDescriptor)
            is ChanDescriptor.CatalogDescriptor -> ChanThreadManager.LoadedChanDescriptor.Regular(currentDescriptor)
            is ChanDescriptor.ThreadDescriptor -> ChanThreadManager.LoadedChanDescriptor.Regular(currentDescriptor)
          }

          val albumItems = getAlbumItemsForChanDescriptor(loadedChanDescriptor)
          Logger.debug(TAG) { "Got ${albumItems?.size ?: 0} initial album items" }

          if (!albumItems.isNullOrEmpty()) {
            updateUi(albumItems)
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
        .filter { newAlbumItems -> newAlbumItems.isNotEmpty() }
        .collectLatest { newAlbumItems -> updateUi(newAlbumItems) }
    }
  }

  fun updateLastScrollPosition(newLastScrollPosition: Int) {
    _lastScrollPosition.intValue = newLastScrollPosition
  }

  fun mapPostImagesToPostDescriptors(): List<PostDescriptor> {
    val duplicateSet = mutableSetOf<PostDescriptor>()

    return albumItems.mapNotNull { postImage ->
      if (duplicateSet.add(postImage.postDescriptor)) {
        return@mapNotNull postImage.postDescriptor
      }

      return@mapNotNull null
    }
  }

  suspend fun findChanPostImage(albumItemData: AlbumItemData): ChanPostImage? {
    val thumbnailImage = albumItemData.thumbnailImage.asUrlOrNull()
    if (thumbnailImage == null) {
      return null
    }

    val chanPost = when (val chanDescriptor = _currentDescriptor.value) {
      is ChanDescriptor.ICatalogDescriptor -> {
        chanThreadManager.getChanCatalog(chanDescriptor)?.getPost(albumItemData.postDescriptor)
      }
      is ChanDescriptor.ThreadDescriptor -> {
        chanThreadManager.getChanThread(chanDescriptor)?.getPost(albumItemData.postDescriptor)
      }
      null -> null
    }

    if (chanPost == null) {
      return null
    }

    return chanPost.firstPostImageOrNull { chanPostImage -> chanPostImage.actualThumbnailUrl == thumbnailImage }
  }

  suspend fun requestScrollToImage(chanPostImage: ChanPostImage) {
    val newPosition = albumItems.indexOfFirst { albumItemData ->
      if (albumItemData.postDescriptor != chanPostImage.ownerPostDescriptor) {
        return@indexOfFirst false
      }

      return@indexOfFirst albumItemData.thumbnailImage.asUrlOrNull() == chanPostImage.actualThumbnailUrl
    }

    if (newPosition < 0) {
      return
    }

    _scrollToPosition.emit(newPosition)
  }

  private suspend fun updateUi(
    allAlbumItems: List<AlbumItemData>
  ) {
    Logger.debug(TAG) { "Got ${allAlbumItems.size} album items" }

    val (duplicateChecker, capacity) = withContext(Dispatchers.Main) {
      val duplicateChecker = _albumItems
        .toHashSetBy(capacity = _albumItems.size) { albumItemData -> albumItemData.thumbnailImage }

      val capacity = (allAlbumItems.size - _albumItems.size).coerceAtLeast(16)
      return@withContext duplicateChecker to capacity
    }

    val newAlbumItemsToAppendList = mutableListWithCap<AlbumItemData>(capacity)

    allAlbumItems.forEach { newAlbumItemData ->
      if (duplicateChecker.contains(newAlbumItemData.thumbnailImage)) {
        return@forEach
      }

      newAlbumItemsToAppendList += newAlbumItemData
    }

    Logger.debug(TAG) { "Got ${newAlbumItemsToAppendList.size} new album items" }

    withContext(Dispatchers.Main) {
      if (newAlbumItemsToAppendList.isNotEmpty()) {
        applyNewAlbumItems(newAlbumItemsToAppendList)
      }

      updateToolbarTitle(
        newAlbumItems = allAlbumItems,
        chanDescriptor = _currentDescriptor.value
      )

      updateScrollPosition(allAlbumItems)
    }
  }

  private suspend fun updateScrollPosition(allAlbumItems: List<AlbumItemData>) {
    val initialImageFullUrl = savedStateHandle
      .getAndConsume<AlbumViewControllerV2.Params> { params -> params.copy(initialImageFullUrl = null) }
      ?.initialImageFullUrl

    if (initialImageFullUrl.isNullOrBlank()) {
      Logger.debug(TAG) { "updateScrollPosition() initialImageFullUrl is null or blank" }
      return
    }

    val indexToScrollTo = allAlbumItems
      .indexOfFirst { albumItemData -> albumItemData.fullImage?.asUrlOrNull()?.toString() == initialImageFullUrl }
    if (indexToScrollTo < 0) {
      Logger.debug(TAG) { "updateScrollPosition() failed to find '${initialImageFullUrl}'" }
      return
    }

    Logger.debug(TAG) { "updateScrollPosition() '${initialImageFullUrl}' found at ${indexToScrollTo}, performing scroll" }
    _scrollToPosition.emit(indexToScrollTo)
    _lastScrollPosition.intValue = indexToScrollTo
  }

  private suspend fun updateToolbarTitle(
    newAlbumItems: List<AlbumItemData>,
    chanDescriptor: ChanDescriptor?
  ) {
    BackgroundUtils.ensureMainThread()

    val toolbarTitle = when (chanDescriptor) {
      is ChanDescriptor.CompositeCatalogDescriptor,
      is ChanDescriptor.CatalogDescriptor -> {
        if (chanDescriptor is ChanDescriptor.CompositeCatalogDescriptor) {
          compositeCatalogManager.byCompositeCatalogDescriptor(chanDescriptor)?.name
            ?: ChanPostUtils.getTitle(null, chanDescriptor)
        } else {
          ChanPostUtils.getTitle(null, chanDescriptor)
        }
      }
      is ChanDescriptor.ThreadDescriptor -> {
        ChanPostUtils.getTitle(
          chanThreadManager.getChanThread(chanDescriptor)?.getOriginalPost(),
          chanDescriptor
        )
      }
      null -> ""
    }
    val toolbarSubTitle = appResources.quantityString(
      R.plurals.image,
      newAlbumItems.size,
      newAlbumItems.size
    )

    _toolbarData.value = ToolbarData(
      title = toolbarTitle,
      subtitle = toolbarSubTitle
    )
  }

  private fun applyNewAlbumItems(newAlbumItemsToAppendList: List<AlbumItemData>) {
    BackgroundUtils.ensureMainThread()

    if (newAlbumItemsToAppendList.isEmpty()) {
      return
    }

    _albumItems.addAll(newAlbumItemsToAppendList)

    // Do not show the toast the first time we open AlbumViewController. Only show it for album item updates.
    if (_skippedInitialLoad) {
      snackbarManager.toast(
        appResources.string(
          R.string.album_screen_new_images,
          newAlbumItemsToAppendList.size
        )
      )
    }

    _skippedInitialLoad = true
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
  data class ToolbarData(
    val title: String = "",
    val subtitle: String = ""
  )

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
      get() {
        val fullImage = fullImage
        if (fullImage != null) {
          return "${postDescriptor.serializeToString()}_${thumbnailImage.uniqueKey()}_${fullImage.uniqueKey()}"
        }

        return "${postDescriptor.serializeToString()}_${thumbnailImage.uniqueKey()}"
      }

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
    private val appResources: AppResources,
    private val currentOpenedDescriptorStateManager: CurrentOpenedDescriptorStateManager,
    private val chanThreadManager: ChanThreadManager,
    private val chanCatalogSnapshotCache: ChanCatalogSnapshotCache,
    private val chanThreadsCache: ChanThreadsCache,
    private val snackbarManagerFactory: SnackbarManagerFactory,
    private val compositeCatalogManager: CompositeCatalogManager
  ) : ViewModelAssistedFactory<AlbumViewControllerV2ViewModel> {
    override fun create(handle: SavedStateHandle): AlbumViewControllerV2ViewModel {
      return AlbumViewControllerV2ViewModel(
        savedStateHandle = handle,
        appResources = appResources,
        currentOpenedDescriptorStateManager = currentOpenedDescriptorStateManager,
        chanThreadManager = chanThreadManager,
        chanCatalogSnapshotCache = chanCatalogSnapshotCache,
        chanThreadsCache = chanThreadsCache,
        snackbarManagerFactory = snackbarManagerFactory,
        compositeCatalogManager = compositeCatalogManager
      )
    }
  }

  companion object {
    private const val TAG = "AlbumViewControllerV2ViewModel"

    private const val MAX_RATIO = 2f
    private const val MIN_RATIO = .4f
  }

}