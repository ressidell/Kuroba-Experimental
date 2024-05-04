package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.helper.ThumbnailLongtapOptionsHelper
import com.github.k1rakishou.chan.core.manager.ChanThreadManager
import com.github.k1rakishou.chan.core.manager.CompositeCatalogManager
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
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
import com.github.k1rakishou.chan.ui.view.insets.ColorizableInsetAwareGridRecyclerView
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.dp
import com.github.k1rakishou.common.AndroidUtils
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPostImage
import com.github.k1rakishou.model.util.ChanPostUtils
import com.github.k1rakishou.persist_state.PersistableChanState.albumLayoutGridMode
import com.github.k1rakishou.persist_state.PersistableChanState.showAlbumViewsImageDetails
import javax.inject.Inject

// TODO: scoped viewmodels. Remove me.
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
  lateinit var compositeCatalogManager: CompositeCatalogManager
  @Inject
  lateinit var albumThreadControllerHelpers: AlbumThreadControllerHelpers

  override fun injectActivityDependencies(component: ActivityComponent) {
    // TODO: scoped viewmodels.
//    component.inject(this)
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
          onClick = { item ->  }
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
            onClick = {  }
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

  private fun downloadAlbumClicked(item: ToolbarMenuItem) {
    val albumDownloadController = AlbumDownloadController(context)
    albumDownloadController.setPostImages(postImages)
    requireNavController().pushController(albumDownloadController)
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
    }

    override fun onLongClick(v: View): Boolean {

      return true
    }

  }

  private class SpanInfo(val spanCount: Int, val spanWidth: Int)

  companion object {
    private val DEFAULT_SPAN_WIDTH = dp(120f)
    private const val ACTION_DOWNLOAD = 0
    private const val ACTION_TOGGLE_LAYOUT_MODE = 1
    private const val ACTION_TOGGLE_IMAGE_DETAILS = 2
  }
}