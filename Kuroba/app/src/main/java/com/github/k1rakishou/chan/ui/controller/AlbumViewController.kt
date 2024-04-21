package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.helper.ThumbnailLongtapOptionsHelper
import com.github.k1rakishou.chan.core.manager.ChanThreadManager
import com.github.k1rakishou.chan.core.manager.CompositeCatalogManager
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.usecase.FilterOutHiddenImagesUseCase
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerActivity
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerOptions
import com.github.k1rakishou.chan.features.media_viewer.helper.AlbumThreadControllerHelpers
import com.github.k1rakishou.chan.features.media_viewer.helper.MediaViewerGoToImagePostHelper
import com.github.k1rakishou.chan.features.media_viewer.helper.MediaViewerGoToPostHelper
import com.github.k1rakishou.chan.features.media_viewer.helper.MediaViewerOpenThreadHelper
import com.github.k1rakishou.chan.features.media_viewer.helper.MediaViewerScrollerHelper
import com.github.k1rakishou.chan.features.settings.screens.AppearanceSettingsScreen.Companion.clampColumnsCount
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.cell.AlbumViewCell
import com.github.k1rakishou.chan.ui.compose.lazylist.ScrollbarView
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableGridRecyclerView
import com.github.k1rakishou.chan.ui.view.FixedLinearLayoutManager
import com.github.k1rakishou.chan.ui.view.insets.ColorizableInsetAwareGridRecyclerView
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.dp
import com.github.k1rakishou.common.AndroidUtils
import com.github.k1rakishou.common.isNotNullNorBlank
import com.github.k1rakishou.common.mutableListWithCap
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.filter.ChanFilterMutable
import com.github.k1rakishou.model.data.post.ChanPostImage
import com.github.k1rakishou.model.util.ChanPostUtils
import com.github.k1rakishou.persist_state.PersistableChanState.albumLayoutGridMode
import com.github.k1rakishou.persist_state.PersistableChanState.showAlbumViewsImageDetails
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import javax.inject.Inject

class AlbumViewController(
  context: Context,
  private val chanDescriptor: ChanDescriptor,
  private val displayingPostDescriptors: List<PostDescriptor>
) : Controller(context) {
  private lateinit var recyclerView: ColorizableInsetAwareGridRecyclerView

  private val postImages = mutableListOf<ChanPostImage>()
  private var targetIndex = -1

  private lateinit var scrollbarView: ScrollbarView
  private var albumAdapter: AlbumAdapter? = null

  override val controllerKey: ControllerKey
    get() = ControllerKey("${this::class.java.name}_${chanDescriptor.userReadableString()}")

  private val spanCountAndSpanWidth: SpanInfo
    get() {
      var albumSpanCount = ChanSettings.albumSpanCount.get()
      var albumSpanWith = DEFAULT_SPAN_WIDTH
      val displayWidth = AndroidUtils.getDisplaySize(context).x

      if (albumSpanCount == 0) {
        albumSpanCount = displayWidth / DEFAULT_SPAN_WIDTH
      } else {
        albumSpanWith = displayWidth / albumSpanCount
      }

      albumSpanCount = clampColumnsCount(albumSpanCount)
      return SpanInfo(albumSpanCount, albumSpanWith)
    }

  @Inject
  lateinit var chanThreadManager: ChanThreadManager
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var thumbnailLongtapOptionsHelper: ThumbnailLongtapOptionsHelper
  @Inject
  lateinit var mediaViewerScrollerHelper: MediaViewerScrollerHelper
  @Inject
  lateinit var mediaViewerGoToImagePostHelper: MediaViewerGoToImagePostHelper
  @Inject
  lateinit var mediaViewerGoToPostHelper: MediaViewerGoToPostHelper
  @Inject
  lateinit var mediaViewerOpenThreadHelper: MediaViewerOpenThreadHelper
  @Inject
  lateinit var filterOutHiddenImagesUseCase: FilterOutHiddenImagesUseCase
  @Inject
  lateinit var compositeCatalogManager: CompositeCatalogManager
  @Inject
  lateinit var albumThreadControllerHelpers: AlbumThreadControllerHelpers

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun onCreate() {
    super.onCreate()

    val toolbarTitle = when (chanDescriptor) {
      is ChanDescriptor.CompositeCatalogDescriptor,
      is ChanDescriptor.CatalogDescriptor -> {
        ChanPostUtils.getTitle(null, chanDescriptor)
      }
      is ChanDescriptor.ThreadDescriptor -> {
        ChanPostUtils.getTitle(
          chanThreadManager.getChanThread(chanDescriptor)?.getOriginalPost(),
          chanDescriptor
        )
      }
    }
    val toolbarSubTitle = AppModuleAndroidUtils.getQuantityString(R.plurals.image, postImages.size, postImages.size)

    val downloadDrawableId = if (albumLayoutGridMode.get()) {
      R.drawable.ic_baseline_view_quilt_24
    } else {
      R.drawable.ic_baseline_view_comfy_24
    }

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.String(toolbarTitle),
        subtitle = ToolbarText.String(toolbarSubTitle)
      ),
      menuBuilder = {
        withMenuItem(
          id = ACTION_TOGGLE_LAYOUT_MODE,
          drawableId = downloadDrawableId,
          onClick = { item -> toggleLayoutModeClicked(item) }
        )
        withMenuItem(
          id = ACTION_DOWNLOAD,
          drawableId = R.drawable.ic_file_download_white_24dp,
          onClick = { item -> downloadAlbumClicked(item) }
        )

        withOverflowMenu {
          withCheckableOverflowMenuItem(
            id = ACTION_TOGGLE_IMAGE_DETAILS,
            stringId = R.string.action_album_show_image_details,
            visible = true,
            checked = showAlbumViewsImageDetails.get(),
            onClick = { onToggleAlbumViewsImageInfoToggled() }
          )
        }
      }
    )

    // View setup
    view = AppModuleAndroidUtils.inflate(context, R.layout.controller_album_view)
    recyclerView = view.findViewById(R.id.recycler_view)
    recyclerView.setHasFixedSize(true)
    albumAdapter = AlbumAdapter()
    recyclerView.adapter = albumAdapter
    updateRecyclerView(false)

    scrollbarView = view.findViewById(R.id.album_view_controller_scrollbar)
    scrollbarView.attachRecyclerView(recyclerView)
    scrollbarView.isScrollbarDraggable(true)

    controllerScope.launch {
      mediaViewerScrollerHelper.mediaViewerScrollEventsFlow
        .collect { scrollToImageEvent ->
          val descriptor = scrollToImageEvent.chanDescriptor
          if (descriptor != chanDescriptor) {
            return@collect
          }

          val index = postImages.indexOf(scrollToImageEvent.chanPostImage)
          if (index < 0) {
            return@collect
          }

          scrollToInternal(index)
        }
    }

    controllerScope.launch {
      mediaViewerGoToImagePostHelper.mediaViewerGoToPostEventsFlow
        .collect { goToPostEvent ->
          val postImage = goToPostEvent.chanPostImage
          val chanDescriptor = goToPostEvent.chanDescriptor

          requireNavController().popController {
            albumThreadControllerHelpers.highlightPostWithImage(chanDescriptor, postImage)
          }
        }
    }

    controllerScope.launch {
      mediaViewerGoToPostHelper.mediaViewerGoToPostEventsFlow
        .collect { postDescriptor ->
          if (postDescriptor.descriptor != chanDescriptor) {
            return@collect
          }

          requireNavController().popController()
        }
    }

    if (chanDescriptor is ChanDescriptor.CompositeCatalogDescriptor) {
      controllerScope.launch {
        val compositeCatalogName = compositeCatalogManager.byCompositeCatalogDescriptor(chanDescriptor)
          ?.name

        if (compositeCatalogName.isNotNullNorBlank()) {
          toolbarState.default.updateTitle(
            newTitle = ToolbarText.String(compositeCatalogName)
          )
        }
      }
    }
  }

  private fun scrollToInternal(scrollPosition: Int) {
    val layoutManager = recyclerView.layoutManager

    if (layoutManager is GridLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
      return
    }

    if (layoutManager is StaggeredGridLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
      return
    }

    if (layoutManager is FixedLinearLayoutManager) {
      layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
      return
    }

    recyclerView.scrollToPosition(scrollPosition)
  }

  private fun updateRecyclerView(reloading: Boolean) {
    val spanInfo = spanCountAndSpanWidth
    val staggeredGridLayoutManager = StaggeredGridLayoutManager(
      spanInfo.spanCount,
      StaggeredGridLayoutManager.VERTICAL
    )

    recyclerView.layoutManager = staggeredGridLayoutManager
    recyclerView.setSpanWidth(spanInfo.spanWidth)
    recyclerView.itemAnimator = null
    recyclerView.scrollToPosition(targetIndex)

    if (reloading) {
      albumAdapter?.refresh()
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    scrollbarView.cleanup()
    recyclerView.swapAdapter(null, true)
  }

  fun tryCollectingImages(initialImageUrl: HttpUrl?): Boolean {
    val (images, index) = collectImages(initialImageUrl)

    if (images.isEmpty()) {
      return false
    }

    val input = FilterOutHiddenImagesUseCase.Input(
      images = images,
      index = index,
      isOpeningAlbum = true,
      postDescriptorSelector = { chanPostImage -> chanPostImage.ownerPostDescriptor }
    )

    val output = filterOutHiddenImagesUseCase.filter(input)
    val filteredImages = output.images
    val newIndex = output.index

    if (filteredImages.isEmpty()) {
      return false
    }

    targetIndex = newIndex

    postImages.clear()
    postImages.addAll(filteredImages)

    return true
  }

  private fun collectImages(initialImageUrl: HttpUrl?): Pair<List<ChanPostImage>, Int> {
    var imageIndexToScroll = 0
    var index = 0

    when (chanDescriptor) {
      is ChanDescriptor.CompositeCatalogDescriptor,
      is ChanDescriptor.CatalogDescriptor -> {
        val postImages = mutableListOf<ChanPostImage>()

        displayingPostDescriptors.forEach { displayingPostDescriptor ->
          val chanPost = chanThreadManager.getPost(displayingPostDescriptor)
          if (chanPost == null) {
            return@forEach
          }

          chanPost.iteratePostImages { chanPostImage ->
            postImages += chanPostImage

            if (initialImageUrl != null && chanPostImage.imageUrl == initialImageUrl) {
              imageIndexToScroll = index
            }

            ++index
          }
        }

        return postImages to imageIndexToScroll
      }
      is ChanDescriptor.ThreadDescriptor -> {
        val chanThread = chanThreadManager.getChanThread(chanDescriptor)
        if (chanThread == null) {
          return emptyList<ChanPostImage>() to imageIndexToScroll
        }

        val postImages = mutableListWithCap<ChanPostImage>(chanThread.postsCount)

        chanThread.iteratePostsOrdered { chanPost ->
          chanPost.iteratePostImages { chanPostImage ->
            postImages += chanPostImage

            if (initialImageUrl != null && chanPostImage.imageUrl == initialImageUrl) {
              imageIndexToScroll = index
            }

            ++index
          }
        }

        return postImages to imageIndexToScroll
      }
    }
  }

  private fun onToggleAlbumViewsImageInfoToggled() {
    toolbarState.findCheckableOverflowItem(ACTION_TOGGLE_IMAGE_DETAILS)
      ?.updateChecked(showAlbumViewsImageDetails.toggle())

    albumAdapter?.refresh()
  }

  private fun downloadAlbumClicked(item: ToolbarMenuItem) {
    val albumDownloadController = AlbumDownloadController(context)
    albumDownloadController.setPostImages(postImages)
    requireNavController().pushController(albumDownloadController)
  }

  private fun toggleLayoutModeClicked(item: ToolbarMenuItem) {
    albumLayoutGridMode.toggle()
    updateRecyclerView(true)

    toolbarState.findItem(ACTION_TOGGLE_LAYOUT_MODE)?.let { toolbarMenuItem ->
      val drawableId = if (albumLayoutGridMode.get()) {
        R.drawable.ic_baseline_view_quilt_24
      } else {
        R.drawable.ic_baseline_view_comfy_24
      }

      toolbarMenuItem.updateDrawableId(drawableId)
    }
  }

  private fun openImage(postImage: ChanPostImage) {
    val index = postImages.indexOf(postImage)
    if (index < 0) {
      return
    }

    when (chanDescriptor) {
      is ChanDescriptor.ICatalogDescriptor -> {
        MediaViewerActivity.catalogMedia(
          context = context,
          catalogDescriptor = chanDescriptor,
          initialImageUrl = postImages[index].imageUrl?.toString(),
          transitionThumbnailUrl = postImages[index].getThumbnailUrl()!!.toString(),
          lastTouchCoordinates = globalWindowInsetsManager.lastTouchCoordinates(),
          mediaViewerOptions = MediaViewerOptions(
            mediaViewerOpenedFromAlbum = true
          )
        )
      }
      is ChanDescriptor.ThreadDescriptor -> {
        MediaViewerActivity.threadMedia(
          context = context,
          threadDescriptor = chanDescriptor,
          postDescriptorList = mapPostImagesToPostDescriptors(),
          initialImageUrl = postImages[index].imageUrl?.toString(),
          transitionThumbnailUrl = postImages[index].getThumbnailUrl()!!.toString(),
          lastTouchCoordinates = globalWindowInsetsManager.lastTouchCoordinates(),
          mediaViewerOptions = MediaViewerOptions(
            mediaViewerOpenedFromAlbum = true
          )
        )
      }
    }
  }

  private fun mapPostImagesToPostDescriptors(): List<PostDescriptor> {
    val duplicateSet = mutableSetOf<PostDescriptor>()

    return postImages.mapNotNull { postImage ->
      if (duplicateSet.add(postImage.ownerPostDescriptor)) {
        return@mapNotNull postImage.ownerPostDescriptor
      }

      return@mapNotNull null
    }
  }

  private fun showImageLongClickOptions(postImage: ChanPostImage) {
    thumbnailLongtapOptionsHelper.onThumbnailLongTapped(
      context = context,
      chanDescriptor = chanDescriptor,
      isCurrentlyInAlbum = true,
      postImage = postImage,
      presentControllerFunc = { controller -> presentController(controller) },
      showFiltersControllerFunc = { },
      openThreadFunc = { postDescriptor ->
        withLayoutMode(
          phone = { requireNavController().popController(false) }
        )

        mediaViewerOpenThreadHelper.tryToOpenThread(postDescriptor)
      },
      goToPostFunc = {
        requireNavController().popController {
          albumThreadControllerHelpers.highlightPostWithImage(chanDescriptor, postImage)
        }
      }
    )
  }

  private inner class AlbumAdapter : RecyclerView.Adapter<AlbumItemCellHolder>() {
    private val albumCellType = 1

    init {
      setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
      return albumCellType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumItemCellHolder {
      val view = AppModuleAndroidUtils.inflate(parent.context, R.layout.cell_album_view, parent, false)
      return AlbumItemCellHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumItemCellHolder, position: Int) {
      val postImage = postImages.get(position)
      val canUseHighResCells = ColorizableGridRecyclerView.canUseHighResCells(recyclerView.currentSpanCount)
      val isStaggeredGridMode = !albumLayoutGridMode.get()

      holder.cell.bindPostImage(
        chanDescriptor = chanDescriptor,
        postImage = postImage,
        canUseHighResCells = canUseHighResCells,
        isStaggeredGridMode = isStaggeredGridMode,
        showDetails = showAlbumViewsImageDetails.get()
      )
    }

    override fun onViewRecycled(holder: AlbumItemCellHolder) {
      holder.cell.unbindPostImage()
    }

    override fun getItemCount(): Int {
      return postImages.size
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    fun refresh() {
      notifyDataSetChanged()
    }
  }

  private inner class AlbumItemCellHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, OnLongClickListener {
    private val ALBUM_VIEW_CELL_THUMBNAIL_CLICK_TOKEN = "ALBUM_VIEW_CELL_THUMBNAIL_CLICK"
    private val ALBUM_VIEW_CELL_THUMBNAIL_LONG_CLICK_TOKEN = "ALBUM_VIEW_CELL_THUMBNAIL_LONG_CLICK"

    val cell = itemView as AlbumViewCell
    val thumbnailView = cell.postImageThumbnailView

    init {
      thumbnailView.setImageClickListener(ALBUM_VIEW_CELL_THUMBNAIL_CLICK_TOKEN, this)
      thumbnailView.setImageLongClickListener(ALBUM_VIEW_CELL_THUMBNAIL_LONG_CLICK_TOKEN, this)
    }

    override fun onClick(v: View) {
      val postImage = postImages.getOrNull(adapterPosition)
        ?: return

      openImage(postImage)
    }

    override fun onLongClick(v: View): Boolean {
      val postImage = postImages.getOrNull(adapterPosition)
        ?: return false

      showImageLongClickOptions(postImage)
      return true
    }

  }

  private class SpanInfo(val spanCount: Int, val spanWidth: Int)

  interface ThreadControllerCallbacks {
    fun openFiltersController(chanFilterMutable: ChanFilterMutable)
  }

  companion object {
    private val DEFAULT_SPAN_WIDTH = dp(120f)
    private const val ACTION_DOWNLOAD = 0
    private const val ACTION_TOGGLE_LAYOUT_MODE = 1
    private const val ACTION_TOGGLE_IMAGE_DETAILS = 2
  }
}